package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Attendance;
import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Payroll;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqPayrollSyncDTO;
import com.quyen.shoplite.domain.response.ResPayrollDTO;
import com.quyen.shoplite.repository.AttendanceRepository;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.PayrollRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.repository.TransactionRepository;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private PayrollRepository    payrollRepository;
    @Mock private EmployeeRepository   employeeRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private RosterRepository     rosterRepository;
    @Mock private TransactionRepository transactionRepository;

    private PayrollService payrollService;
    private Employee       employee;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(payrollRepository, employeeRepository, attendanceRepository, rosterRepository, transactionRepository);
        employee = Employee.builder()
                .id(1).salaryRate(100.0)
                .user(User.builder().id(1).username("emp1").build())
                .build();
    }

    @Test
    void syncMonthlyPayroll_SumsPayableMinutesAndPersistsTotalSalary() {
        // Arrange: two completed shifts — 480 min (8h) + 450 min (7.5h) = 930 min = 15.5h
        // totalSalary = 15.5 * 100 + 50 - 10 = 1590
        ReqPayrollSyncDTO req = new ReqPayrollSyncDTO();
        req.setEmployeeId(1);
        req.setPeriod(LocalDate.of(2025, 1, 18));
        req.setBonus(50.0);
        req.setPenalty(10.0);

        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));

        when(attendanceRepository.findCompletedByEmployeeAndPeriod(
                1,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)))
                .thenReturn(List.of(
                        Attendance.builder()
                                .workingDay(LocalDate.of(2025, 1, 10))
                                .payableMinutes(480L)
                                .build(),
                        Attendance.builder()
                                .workingDay(LocalDate.of(2025, 1, 11))
                                .payableMinutes(450L)
                                .build()
                ));

        // No roster entries → no absent penalties
        when(rosterRepository.findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
                1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .thenReturn(Collections.emptyList());

        when(payrollRepository.findByEmployee_IdAndPeriod(1, LocalDate.of(2025, 1, 1)))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> {
            Payroll p = inv.getArgument(0);
            p.setId(10);
            return p;
        });
        when(transactionRepository.findByPayroll_IdAndType(10, TypeTransactionEnum.SALARY))
                .thenReturn(Optional.empty());

        // Act
        List<ResPayrollDTO> result = payrollService.syncMonthlyPayroll(req);

        // Assert
        assertEquals(1, result.size());
        ResPayrollDTO dto = result.get(0);
        assertEquals(LocalDate.of(2025, 1, 1), dto.getPeriod());
        assertEquals(15.5, dto.getTotalHours(), 0.001);
        assertEquals(1590.0, dto.getTotalSalary(), 0.001);
    }

    @Test
    void syncMonthlyPayroll_ThrowsBadRequest_WhenSalaryRateIsNegative() {
        employee.setSalaryRate(-10.0);
        ReqPayrollSyncDTO req = new ReqPayrollSyncDTO();
        req.setEmployeeId(1);
        req.setPeriod(LocalDate.of(2025, 1, 18));

        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));

        com.quyen.shoplite.util.error.BadRequestException ex = assertThrows(
                com.quyen.shoplite.util.error.BadRequestException.class,
                () -> payrollService.syncMonthlyPayroll(req)
        );
        assertTrue(ex.getMessage().contains("negative or missing salary rate"));
    }

    @Test
    void syncMonthlyPayroll_ThrowsBadRequest_WhenTotalSalaryIsNegative() {
        ReqPayrollSyncDTO req = new ReqPayrollSyncDTO();
        req.setEmployeeId(1);
        req.setPeriod(LocalDate.of(2025, 1, 18));
        req.setPenalty(2000.0); // Make total salary negative

        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findCompletedByEmployeeAndPeriod(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(rosterRepository.findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        com.quyen.shoplite.util.error.BadRequestException ex = assertThrows(
                com.quyen.shoplite.util.error.BadRequestException.class,
                () -> payrollService.syncMonthlyPayroll(req)
        );
        assertTrue(ex.getMessage().contains("Total salary cannot be negative"));
    }

    @Test
    void syncMonthlyPayroll_WithLeaveApproved_AddsExpectedMinutes() {
        ReqPayrollSyncDTO req = new ReqPayrollSyncDTO();
        req.setEmployeeId(1);
        req.setPeriod(LocalDate.of(2025, 1, 1));

        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findCompletedByEmployeeAndPeriod(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        com.quyen.shoplite.domain.Roster roster = com.quyen.shoplite.domain.Roster.builder()
                .type(com.quyen.shoplite.util.constant.RosterTypeEnum.LEAVE_APPROVED)
                .workingDay(LocalDate.of(2025, 1, 5))
                .expectedHours(8.0)
                .build();

        when(rosterRepository.findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(any(), any(), any()))
                .thenReturn(List.of(roster));

        when(payrollRepository.findByEmployee_IdAndPeriod(any(), any())).thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ResPayrollDTO> result = payrollService.syncMonthlyPayroll(req);

        assertEquals(1, result.size());
        assertEquals(8.0, result.get(0).getTotalHours(), 0.001);
        assertEquals(1, result.get(0).getApprovedLeaveDays());
    }

    @Test
    void findById_Success() {
        Payroll p = Payroll.builder().id(10).period(LocalDate.now()).employee(employee).build();
        when(payrollRepository.findById(10)).thenReturn(Optional.of(p));

        ResPayrollDTO res = payrollService.findById(10);
        assertNotNull(res);
        assertEquals(10, res.getId());
    }

    @Test
    void findById_ThrowsNotFound() {
        when(payrollRepository.findById(10)).thenReturn(Optional.empty());
        assertThrows(com.quyen.shoplite.util.error.ResourceNotFoundException.class, () -> payrollService.findById(10));
    }
}
