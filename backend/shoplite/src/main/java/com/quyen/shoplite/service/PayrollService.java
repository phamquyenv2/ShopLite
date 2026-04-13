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
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
     */
    @Transactional
    public List<ResPayrollDTO> syncMonthlyPayroll(ReqPayrollSyncDTO req) {
        LocalDate period    = req.getPeriod().withDayOfMonth(1);
        LocalDate periodEnd = period.withDayOfMonth(period.lengthOfMonth());

        List<Employee> employees = req.getEmployeeId() != null
                ? List.of(findEmployee(req.getEmployeeId()))
                : employeeRepository.findAllByDeletedFalseOrderByIdAsc();

        double bonusGlobal      = req.getBonus()           != null ? req.getBonus()           : 0.0;
        double penaltyGlobal    = req.getPenalty()         != null ? req.getPenalty()         : 0.0;
        double penaltyPerAbsent = req.getPenaltyPerAbsent() != null ? req.getPenaltyPerAbsent() : 0.0;

        List<ResPayrollDTO> results = new ArrayList<>();

        for (Employee employee : employees) {

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
                    .findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
                            employee.getId(), period, periodEnd);

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

            // Upsert Payroll record
            Payroll payroll = payrollRepository.findByEmployee_IdAndPeriod(employee.getId(), period)
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

    public List<ResPayrollDTO> findAll() {
        return payrollRepository.findAllByOrderByPeriodDescEmployee_IdAsc()
                .stream()
                .map(DTOMapper::toResPayrollDTO)
                .toList();
    }

    public List<ResPayrollDTO> findByEmployee(Integer employeeId) {
        findEmployee(employeeId);
        return payrollRepository.findByEmployee_Id(employeeId)
                .stream()
                .map(DTOMapper::toResPayrollDTO)
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private Employee findEmployee(Integer employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id=" + employeeId));
    }
}

