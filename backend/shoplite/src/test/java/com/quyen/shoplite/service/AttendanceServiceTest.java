package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.AttendanceRepository;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Attendance;
import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckInDTO;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckOutDTO;
import com.quyen.shoplite.domain.response.ResAttendanceDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RosterRepository rosterRepository;
    @Mock private UserRepository userRepository;
    @Mock private Clock applicationClock;
    @Mock private CurrentStoreService currentStoreService;

    private AttendanceService service;

    @Captor
    private ArgumentCaptor<Attendance> attendanceCaptor;

    private User user;
    private Employee employee;
    private Office office;

    private final String currentUsername = "testuser";
    private final ZoneId zoneId = ZoneId.of("UTC");

    @BeforeEach
    void setUp() {
        service = new AttendanceService(
                attendanceRepository, employeeRepository, rosterRepository, userRepository, applicationClock, currentStoreService);

        user = User.builder().id(1).username(currentUsername).build();
        Store store = Store.builder().id(1L).name("Test Store").owner(user).build();
        StoreMember storeMember = StoreMember.builder().id(1L).store(store).user(user).build();
        office = Office.builder()
                .id(10)
                .store(store)
                .name("Main Office")
                .officeLat(BigDecimal.valueOf(10.0))
                .officeLng(BigDecimal.valueOf(20.0))
                .radius(100)
                .build();

        employee = Employee.builder()
                .id(100)
                .store(store)
                .storeMember(storeMember)
                .office(office)
                .build();
        lenient().when(currentStoreService.getCurrentStoreId()).thenReturn(store.getId());
    }

    private void setClockTime(LocalDateTime time) {
        Clock fixedClock = Clock.fixed(time.atZone(zoneId).toInstant(), zoneId);
        lenient().when(applicationClock.instant()).thenReturn(fixedClock.instant());
        lenient().when(applicationClock.getZone()).thenReturn(fixedClock.getZone());
    }

    // =========================================================================================
    // Success cases
    // =========================================================================================

    @Test
    void checkIn_SuccessWithinRadius() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);

            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.empty());

            LocalDateTime now = LocalDateTime.of(2026, 4, 13, 8, 0);
            setClockTime(now);

            // Provide a roster with a valid check-in window: start=07:30, end=16:30
            Roster roster = new Roster();
            roster.setId(1);
            roster.setEmployee(employee);
            roster.setWorkingDay(now.toLocalDate());
            roster.setStartTime(LocalTime.of(7, 30));
            roster.setEndTime(LocalTime.of(16, 30));

            when(rosterRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(
                    1L, employee.getId(), now.toLocalDate())).thenReturn(List.of(roster));
            when(attendanceRepository.existsByRoster_Id(roster.getId())).thenReturn(false);
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
            req.setLatitude(10.0);
            req.setLongitude(20.0);

            service.checkIn(req);

            verify(attendanceRepository).save(attendanceCaptor.capture());
            Attendance saved = attendanceCaptor.getValue();

            assertEquals(AttendanceStatusEnum.VALID, saved.getStatus());
            assertTrue(saved.getDistance() <= office.getRadius());
            assertFalse(saved.isWalkIn()); // service hardcodes walkIn=false
            assertEquals(roster, saved.getRoster());
            assertEquals(now, saved.getCheckIn());
        }
    }

    @Test
    void checkIn_RejectsOutOfZone_StillSavesToDb() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);

            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.empty());

            LocalDateTime now = LocalDateTime.of(2026, 4, 13, 8, 0);
            setClockTime(now);

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
            req.setLatitude(11.0); // Far away (> 100m radius)
            req.setLongitude(20.0);

            // Roster with valid window so checkIn proceeds past roster check
            Roster roster = new Roster();
            roster.setId(1);
            roster.setEmployee(employee);
            roster.setWorkingDay(now.toLocalDate());
            roster.setStartTime(LocalTime.of(7, 30));
            roster.setEndTime(LocalTime.of(16, 30));

            when(rosterRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(
                    1L, employee.getId(), now.toLocalDate())).thenReturn(List.of(roster));
            when(attendanceRepository.existsByRoster_Id(roster.getId())).thenReturn(false);
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.checkIn(req));
            assertTrue(ex.getMessage().contains("OUT_OF_ZONE"));

            verify(attendanceRepository).save(attendanceCaptor.capture());
            Attendance saved = attendanceCaptor.getValue();
            assertEquals(AttendanceStatusEnum.OUT_OF_ZONE, saved.getStatus());
            assertTrue(saved.getDistance() > office.getRadius());
        }
    }

    @Test
    void checkIn_SecondShift_Success() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);

            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.empty());

            LocalDateTime now = LocalDateTime.of(2026, 4, 13, 14, 0); // 14:00
            setClockTime(now);

            // Second shift: starts 13:30, ends 22:00
            Roster roster2 = new Roster();
            roster2.setId(2);
            roster2.setEmployee(employee);
            roster2.setWorkingDay(now.toLocalDate());
            roster2.setStartTime(LocalTime.of(13, 30));
            roster2.setEndTime(LocalTime.of(22, 0));

            when(rosterRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(
                    1L, employee.getId(), now.toLocalDate())).thenReturn(List.of(roster2));
            when(attendanceRepository.existsByRoster_Id(roster2.getId())).thenReturn(false);
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
            req.setLatitude(10.0);
            req.setLongitude(20.0);

            service.checkIn(req);

            verify(attendanceRepository).save(attendanceCaptor.capture());
            Attendance saved = attendanceCaptor.getValue();

            assertEquals(roster2, saved.getRoster());
            assertFalse(saved.isWalkIn());
            assertEquals(AttendanceStatusEnum.VALID, saved.getStatus());
        }
    }

    @Test
    void checkOut_SuccessWithWalkIn() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));

            LocalDateTime checkInTime = LocalDateTime.of(2026, 4, 13, 8, 0);
            Attendance openAttendance = Attendance.builder()
                    .id(50)
                    .employee(employee)
                    .office(office)
                    .checkIn(checkInTime)
                    .workingDay(checkInTime.toLocalDate())
                    .status(AttendanceStatusEnum.VALID)
                    .walkIn(true)
                    .build();

            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.of(openAttendance));

            LocalDateTime checkOutTime = LocalDateTime.of(2026, 4, 13, 17, 0);
            setClockTime(checkOutTime);

            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();
            req.setLatitude(10.0);
            req.setLongitude(20.0);

            service.checkOut(req);

            verify(attendanceRepository).save(attendanceCaptor.capture());
            Attendance saved = attendanceCaptor.getValue();

            assertEquals(checkOutTime, saved.getCheckOut());
            assertEquals(AttendanceStatusEnum.VALID, saved.getStatus());
            assertEquals(540L, saved.getWorkedMinutes());
            assertEquals(540L, saved.getPayableMinutes()); // walk-in = worked
            assertEquals(BigDecimal.valueOf(10.0), saved.getCheckOutLatitude());
            assertFalse(saved.isClosedAutomatically());
        }
    }

    @Test
    void checkOut_ComputedMinutesForRoster() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));

            Roster roster = new Roster();
            roster.setStartTime(LocalTime.of(9, 0));
            roster.setEndTime(LocalTime.of(17, 0));
            roster.setUnpaidBreakMinutes(30L);

            // check-in 9:20 (after shift start 9:00)
            LocalDateTime checkInTime = LocalDateTime.of(2026, 4, 13, 9, 20);
            Attendance openAttendance = Attendance.builder()
                    .employee(employee)
                    .office(office)
                    .roster(roster)
                    .checkIn(checkInTime)
                    .workingDay(checkInTime.toLocalDate())
                    .walkIn(false)
                    .status(AttendanceStatusEnum.VALID)
                    .build();

            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.of(openAttendance));

            // Checkout early 16:45
            LocalDateTime checkOutTime = LocalDateTime.of(2026, 4, 13, 16, 45);
            setClockTime(checkOutTime);

            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();
            req.setLatitude(10.0);
            req.setLongitude(20.0);

            service.checkOut(req);

            verify(attendanceRepository).save(attendanceCaptor.capture());
            Attendance saved = attendanceCaptor.getValue();

            // worked = 9:20 to 16:45 = 7h25m = 445m
            assertEquals(445L, saved.getWorkedMinutes());
            // late = 9:20 - 9:00 = 20m
            assertEquals(20L, saved.getLateMinutes());
            // early = 17:00 - 16:45 = 15m
            assertEquals(15L, saved.getEarlyLeaveMinutes());
            // payable = effective(9:20) to 16:45 = 445m - 30m break = 415m
            assertEquals(415L, saved.getPayableMinutes());
        }
    }

    @Test
    void checkOut_SetsOutOfZone_WhenDistanceTooFar() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));

            LocalDateTime checkInTime = LocalDateTime.of(2026, 4, 13, 8, 0);
            Attendance openAttendance = Attendance.builder()
                    .employee(employee)
                    .office(office)
                    .checkIn(checkInTime)
                    .workingDay(checkInTime.toLocalDate())
                    .status(AttendanceStatusEnum.VALID)
                    .build();

            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.of(openAttendance));
            setClockTime(checkInTime.plusHours(1));

            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

            ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();
            req.setLatitude(11.0); // Far from 10.0
            req.setLongitude(20.0);

            service.checkOut(req);

            verify(attendanceRepository).save(attendanceCaptor.capture());
            assertEquals(AttendanceStatusEnum.OUT_OF_ZONE, attendanceCaptor.getValue().getStatus());
        }
    }

    @Test
    void haversineMetres_CalculatesCorrectly() {
        double dist = AttendanceService.haversineMetres(10.0, 20.0, 10.0, 20.01);
        assertEquals(1096.0, dist, 5.0);
    }

    // =========================================================================================
    // Failure cases
    // =========================================================================================

    @Test
    void checkIn_Throws_WhenEmployeeNotFound() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.empty());

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.checkIn(req));
            assertTrue(ex.getMessage().contains("Employee not found"));
        }
    }

    @Test
    void checkIn_Throws_WhenUserNotFound() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.empty());

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.checkIn(req));
            assertTrue(ex.getMessage().contains("User not found"));
        }
    }

    @Test
    void checkIn_Throws_WhenOfficeMissing() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            employee.setOffice(null);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.checkIn(req));
            assertTrue(ex.getMessage().contains("no office assigned"));
        }
    }

    @Test
    void checkIn_Throws_WhenOpenShiftExists() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));

            setClockTime(LocalDateTime.of(2026, 4, 13, 8, 0));
            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.of(new Attendance()));

            ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.checkIn(req));
            assertTrue(ex.getMessage().contains("open shift"));
        }
    }

    @Test
    void checkOut_Throws_WhenNoOpenShift() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.empty());

            ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.checkOut(req));
            assertTrue(ex.getMessage().contains("No open shift"));
        }
    }

    @Test
    void checkOut_Throws_WhenCheckOutBeforeCheckIn() {
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::requireCurrentUserLogin).thenReturn(currentUsername);
            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(1L, user.getId())).thenReturn(Optional.of(employee));

            LocalDateTime checkInTime = LocalDateTime.of(2026, 4, 13, 10, 0);
            Attendance openAttendance = Attendance.builder()
                    .employee(employee)
                    .office(office)
                    .checkIn(checkInTime)
                    .build();

            when(attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(1L, employee.getId())).thenReturn(Optional.of(openAttendance));

            // checkOut before checkIn
            LocalDateTime checkOutTime = LocalDateTime.of(2026, 4, 13, 9, 0);
            setClockTime(checkOutTime);

            ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.checkOut(req));
            assertTrue(ex.getMessage().contains("check_out must be after check_in"));
        }
    }

    @Test
    void autoCloseAttendance_ClampsNegativeTime() {
        LocalDateTime checkInTime = LocalDateTime.of(2026, 4, 13, 10, 0);
        Roster roster = new Roster();
        roster.setStartTime(LocalTime.of(9, 0));
        roster.setEndTime(LocalTime.of(17, 0));
        roster.setUnpaidBreakMinutes(60L);

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .office(office)
                .roster(roster)
                .checkIn(checkInTime)
                .workingDay(checkInTime.toLocalDate())
                .build();

        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        // Closes BEFORE check-in
        LocalDateTime closeTime = LocalDateTime.of(2026, 4, 13, 9, 0);

        Attendance saved = service.autoCloseAttendance(attendance, closeTime);

        assertEquals(0L, saved.getWorkedMinutes());
        assertEquals(0L, saved.getPayableMinutes());
        assertTrue(saved.isClosedAutomatically());
        assertEquals(AttendanceStatusEnum.NO_CHECK_OUT, saved.getStatus());
    }
}
