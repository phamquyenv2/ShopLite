package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Attendance;
import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Payroll;
import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.domain.request.ReqPayrollSyncDTO;
import com.quyen.shoplite.domain.response.ResPayrollDTO;
import com.quyen.shoplite.repository.AttendanceRepository;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.PayrollRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final RosterRepository rosterRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentStoreService currentStoreService;
    private final UserRepository userRepository;

    // ------------------------------------------------------------------ sync

    /**
     * Tính lương tháng.
     *
     * Luồng chính:
     *  1. Lấy tất cả Attendance đã hoàn thành (checkOut IS NOT NULL) trong kỳ.
     *  2. Cộng payableMinutes từ từng bản ghi Attendance.
     *  3. Với các ngày WORKING trong Roster mà không có Attendance: tính là absent → cộng penaltyPerAbsent.
     *  4. Với ngày LEAVE_APPROVED: cộng expectedMinutes (= expectedHours * 60) vào totalPayableMinutes.
     *  5. totalSalary = (totalPayableMinutes / 60.0) * salaryRate + bonus - penalty
     *
     * NOTE: Việc tạo Payment + Transaction cho lương sẽ do frontend gọi POST /api/v1/payment
     * với referenceType=PAYROLL khi xác nhận chi lương.
     */
    @Transactional
    public List<ResPayrollDTO> syncMonthlyPayroll(ReqPayrollSyncDTO req) {
        LocalDate period    = req.getPeriod().withDayOfMonth(1);
        LocalDate periodEnd = period.withDayOfMonth(period.lengthOfMonth());
        Long storeId = currentStoreService.getCurrentStoreId();

        List<Employee> employees = req.getEmployeeId() != null
                ? List.of(findEmployee(req.getEmployeeId()))
                : employeeRepository.findAllByStoreMember_Store_IdAndDeletedFalseOrderByIdAsc(storeId);

        double bonusGlobal      = req.getBonus()           != null ? req.getBonus()           : 0.0;
        double penaltyGlobal    = req.getPenalty()         != null ? req.getPenalty()         : 0.0;
        double penaltyPerAbsent = req.getPenaltyPerAbsent() != null ? req.getPenaltyPerAbsent() : 0.0;

        List<ResPayrollDTO> results = new ArrayList<>();

        for (Employee employee : employees) {
            if (employee.getSalaryRate() == null || employee.getSalaryRate() < 0) {
                throw new BadRequestException("Employee ID " + employee.getId() + " has invalid negative or missing salary rate");
            }

            // --- 1. Completed Attendance records — source of truth for worked time ---
            List<Attendance> completed = attendanceRepository
                    .findCompletedByEmployeeAndPeriod(employee.getId(), period, periodEnd);

            // Sum payableMinutes from all completed shifts
            long totalPayableMinutes = completed.stream()
                    .map(Attendance::getPayableMinutes)
                    .filter(m -> m != null && m > 0)
                    .mapToLong(Long::longValue)
                    .sum();

            int actualPresentDays = (int) completed.stream()
                    .map(Attendance::getWorkingDay)
                    .distinct()
                    .count();

            // --- 2. Roster-derived adjustments ---
            List<Roster> rosters = rosterRepository
                    .findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
                            storeId, employee.getId(), period, periodEnd);

            // Build set of days that have at least one completed attendance
            java.util.Set<LocalDate> daysWithPresence = completed.stream()
                    .map(Attendance::getWorkingDay)
                    .collect(Collectors.toSet());

            double absentPenalty       = 0.0;
            int scheduledWorkingDays   = 0;
            int approvedLeaveDays      = 0;
            int absentWithoutLeaveDays = 0;

            for (Roster roster : rosters) {
                switch (roster.getType()) {

                    case WORKING -> {
                        scheduledWorkingDays++;
                        if (!daysWithPresence.contains(roster.getWorkingDay())) {
                            // Ngày WORKING nhưng không có Attendance → absence
                            absentWithoutLeaveDays++;
                            absentPenalty += penaltyPerAbsent;
                        }
                    }

                    case LEAVE_APPROVED -> {
                        approvedLeaveDays++;
                        // Cộng payable tương ứng số giờ kỳ vọng của ca vào tổng lương
                        if (roster.getExpectedHours() != null && roster.getExpectedHours() > 0) {
                            totalPayableMinutes += Math.round(roster.getExpectedHours() * 60);
                        }
                    }

                    case LEAVE_UNAPPROVED -> {
                        absentWithoutLeaveDays++;
                        absentPenalty += penaltyPerAbsent;
                    }

                    case OFF -> { /* Không tính gì */ }
                }
            }

            double totalHours  = totalPayableMinutes / 60.0;
            double bonus       = bonusGlobal;
            double penalty     = penaltyGlobal + absentPenalty;
            double totalSalary = totalHours * employee.getSalaryRate() + bonus - penalty;

            if (totalSalary < 0) {
                throw new BadRequestException("Total salary cannot be negative for employee ID " + employee.getId());
            }

            // Upsert Payroll record
            Payroll payroll = payrollRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndPeriod(storeId, employee.getId(), period)
                    .orElseGet(() -> Payroll.builder()
                            .employee(employee)
                            .period(period)
                            .build());

            payroll.setSalaryRate(employee.getSalaryRate());
            payroll.setTotalHours(totalHours);
            payroll.setBonus(bonus);
            payroll.setPenalty(penalty);
            payroll.setTotalSalary(totalSalary);

            Payroll saved = payrollRepository.save(payroll);

            // NOTE: Không tạo Transaction trực tiếp ở đây nữa.
            // Khi cần chi lương, frontend sẽ gọi POST /api/v1/payment với:
            //   referenceType=PAYROLL, referenceId=payroll.id, fundAccountId=..., amount=totalSalary

            ResPayrollDTO dto = DTOMapper.toResPayrollDTO(saved);
            dto.setScheduledWorkingDays(scheduledWorkingDays);
            dto.setActualPresentDays(actualPresentDays);
            dto.setApprovedLeaveDays(approvedLeaveDays);
            dto.setAbsentWithoutLeaveDays(absentWithoutLeaveDays);
            results.add(dto);
        }

        return results;
    }

    // ------------------------------------------------------------------ read

    public ResPayrollDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Payroll payroll = payrollRepository.findById(id)
                .filter(p -> p.getEmployee().getStoreMember().getStore().getId().equals(storeId))
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id=" + id));
        return DTOMapper.toResPayrollDTO(payroll);
    }

    public List<ResPayrollDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return payrollRepository.findAllByEmployee_StoreMember_Store_IdOrderByPeriodDescEmployee_IdAsc(storeId)
                .stream()
                .map(DTOMapper::toResPayrollDTO)
                .toList();
    }

    public List<ResPayrollDTO> findMine() {
        Employee employee = findCurrentEmployee();
        Long storeId = currentStoreService.getCurrentStoreId();
        return payrollRepository.findByEmployee_StoreMember_Store_IdAndEmployee_Id(storeId, employee.getId())
                .stream()
                .map(DTOMapper::toResPayrollDTO)
                .toList();
    }

    public List<ResPayrollDTO> findByEmployee(Integer employeeId) {
        findEmployee(employeeId);
        Long storeId = currentStoreService.getCurrentStoreId();
        return payrollRepository.findByEmployee_StoreMember_Store_IdAndEmployee_Id(storeId, employeeId)
                .stream()
                .map(DTOMapper::toResPayrollDTO)
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private Employee findEmployee(Integer employeeId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(employeeId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id=" + employeeId));
    }

    private Employee findCurrentEmployee() {
        String username = SecurityUtil.requireCurrentUserLogin();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username=" + username));
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(storeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user id=" + user.getId()));
    }
}
