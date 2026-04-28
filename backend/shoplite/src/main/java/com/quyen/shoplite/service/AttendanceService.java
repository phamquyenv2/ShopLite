package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Attendance;
import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckInDTO;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckOutDTO;
import com.quyen.shoplite.domain.response.ResAttendanceDTO;
import com.quyen.shoplite.repository.AttendanceRepository;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final long CHECK_IN_EARLY_WINDOW_MINUTES = 30;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final RosterRepository rosterRepository;
    private final UserRepository userRepository;
    private final Clock applicationClock;
    private final CurrentStoreService currentStoreService;

    // ------------------------------------------------------------------ check-in

    @Transactional
    public ResAttendanceDTO checkIn(ReqAttendanceCheckInDTO req) {
        Employee employee = findCurrentEmployee();
        Office   office   = requireOffice(employee);
        LocalDateTime now   = LocalDateTime.now(applicationClock);
        LocalDate     today = now.toLocalDate();

        Long storeId = employee.getStoreMember().getStore().getId();
        if (attendanceRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(storeId, employee.getId()).isPresent()) {
            throw new BadRequestException(
                    "Employee id=" + employee.getId() + " has an open shift -- please check-out first");
        }

        double distanceMetres = calculateDistanceToOffice(req.getLatitude(), req.getLongitude(), office);
        AttendanceStatusEnum status = distanceMetres <= office.getRadius()
                ? AttendanceStatusEnum.VALID
                : AttendanceStatusEnum.OUT_OF_ZONE;

        LocalTime nowTime     = now.toLocalTime();
        LocalTime windowStart = nowTime.minusMinutes(CHECK_IN_EARLY_WINDOW_MINUTES);
        List<Roster> matching = rosterRepository.findMatchingRoster(
                employee.getId(), today, nowTime, windowStart);
        Roster  roster = matching.isEmpty() ? null : matching.get(0);
        boolean walkIn = (roster == null);

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .office(office)
                .roster(roster)
                .checkIn(now)
                .workingDay(today)
                .walkIn(walkIn)
                .latitude(BigDecimal.valueOf(req.getLatitude()))
                .longitude(BigDecimal.valueOf(req.getLongitude()))
                .distance(distanceMetres)
                .status(status)
                .build();

        return DTOMapper.toResAttendanceDTO(attendanceRepository.save(attendance));
    }

    // ------------------------------------------------------------------ check-out

    @Transactional
    public ResAttendanceDTO checkOut(ReqAttendanceCheckOutDTO req) {
        Employee employee = findCurrentEmployee();
        Long storeId = employee.getStoreMember().getStore().getId();

        Attendance attendance = attendanceRepository
                .findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(storeId, employee.getId())
                .orElseThrow(() -> new BadRequestException(
                        "No open shift found for employee id=" + employee.getId()
                        + " -- please check-in first"));

        LocalDateTime checkOutTime = LocalDateTime.now(applicationClock);

        if (!checkOutTime.isAfter(attendance.getCheckIn())) {
            throw new BadRequestException("check_out must be after check_in");
        }

        double checkoutDistance = calculateDistanceToOffice(
                req.getLatitude(), req.getLongitude(), attendance.getOffice());

        if (checkoutDistance > attendance.getOffice().getRadius()) {
            attendance.setStatus(AttendanceStatusEnum.OUT_OF_ZONE);
        }

        attendance.setCheckOut(checkOutTime);
        attendance.setCheckOutLatitude(BigDecimal.valueOf(req.getLatitude()));
        attendance.setCheckOutLongitude(BigDecimal.valueOf(req.getLongitude()));
        attendance.setCheckOutDistance(checkoutDistance);
        attendance.setClosedAutomatically(false);

        finalizeAttendance(attendance, checkOutTime);

        return DTOMapper.toResAttendanceDTO(attendanceRepository.save(attendance));
    }

    // ------------------------------------------------------------------ read

    public ResAttendanceDTO getTodayForCurrentUser() {
        Employee employee = findCurrentEmployee();
        Long storeId = employee.getStoreMember().getStore().getId();
        List<Attendance> todayList = attendanceRepository
                .findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDayOrderByCheckInDesc(
                        storeId, employee.getId(), LocalDate.now(applicationClock));
        return todayList.isEmpty() ? null : DTOMapper.toResAttendanceDTO(todayList.get(0));
    }

    public ResAttendanceDTO findById(Integer id) {
        return DTOMapper.toResAttendanceDTO(findEntity(id));
    }

    public List<ResAttendanceDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return attendanceRepository.findAllByEmployee_StoreMember_Store_IdOrderByWorkingDayDescCheckInDesc(storeId)
                .stream()
                .map(DTOMapper::toResAttendanceDTO)
                .toList();
    }

    public List<Attendance> findOpenAttendancesUpTo(LocalDate workingDayInclusive) {
        return attendanceRepository.findByCheckOutIsNullAndWorkingDayLessThanEqual(workingDayInclusive);
    }

    public LocalDate currentDate() {
        return LocalDate.now(applicationClock);
    }

    public LocalDateTime currentDateTime() {
        return LocalDateTime.now(applicationClock);
    }

    public LocalDateTime autoCheckoutTimeFor(Attendance attendance) {
        LocalTime autoCheckout = attendance.getOffice().getAutoCheckoutTime() != null
                ? attendance.getOffice().getAutoCheckoutTime()
                : LocalTime.of(23, 59);
        LocalDateTime limitDT = attendance.getWorkingDay().atTime(autoCheckout);

        Roster roster = attendance.getRoster();
        if (roster != null && roster.getStartTime() != null && roster.getEndTime() != null) {
            if (!roster.getEndTime().isAfter(roster.getStartTime())) {
                limitDT = limitDT.plusDays(1);
            }
        }
        return limitDT;
    }

    @Transactional
    public Attendance autoCloseAttendance(Attendance attendance, LocalDateTime closeTime) {
        attendance.setCheckOut(closeTime);
        attendance.setCheckOutLatitude(null);
        attendance.setCheckOutLongitude(null);
        attendance.setCheckOutDistance(null);
        attendance.setClosedAutomatically(true);
        attendance.setStatus(AttendanceStatusEnum.NO_CHECK_OUT);
        finalizeAttendance(attendance, closeTime);
        return attendanceRepository.save(attendance);
    }

    // ------------------------------------------------------------------ helpers

    private void finalizeAttendance(Attendance attendance, LocalDateTime checkOutTime) {
        long workedMinutes = Duration.between(attendance.getCheckIn(), checkOutTime).toMinutes();
        if (workedMinutes < 0) workedMinutes = 0L;

        long payableMinutes;
        long lateMinutes       = 0L;
        long earlyLeaveMinutes = 0L;

        Roster roster = attendance.getRoster();
        Office office = attendance.getOffice();

        if (roster != null && roster.getStartTime() != null && roster.getEndTime() != null) {
            LocalDate     day          = attendance.getWorkingDay();
            LocalDateTime shiftStartDT = day.atTime(roster.getStartTime());
            LocalDateTime shiftEndDT   = day.atTime(roster.getEndTime());

            if (!roster.getEndTime().isAfter(roster.getStartTime())) {
                shiftEndDT = shiftEndDT.plusDays(1);
            }

            int grace = office.getLateGraceMinutes() != null ? office.getLateGraceMinutes() : 0;
            LocalDateTime graceDeadline = shiftStartDT.plusMinutes(grace);
            if (attendance.getCheckIn().isAfter(graceDeadline)) {
                lateMinutes = Duration.between(graceDeadline, attendance.getCheckIn()).toMinutes();
            }

            if (checkOutTime.isBefore(shiftEndDT)) {
                earlyLeaveMinutes = Duration.between(checkOutTime, shiftEndDT).toMinutes();
            }

            LocalDateTime effectiveStart = attendance.getCheckIn().isBefore(shiftStartDT) ? shiftStartDT : attendance.getCheckIn();
            LocalDateTime effectiveEnd = checkOutTime.isAfter(shiftEndDT) ? shiftEndDT : checkOutTime;
            payableMinutes = Duration.between(effectiveStart, effectiveEnd).toMinutes();

            Long unpaidBreak = roster.getUnpaidBreakMinutes() != null ? roster.getUnpaidBreakMinutes() : 0L;
            payableMinutes -= unpaidBreak;

            if (payableMinutes < 0) payableMinutes = 0L;

        } else {
            payableMinutes = workedMinutes;
        }

        attendance.setWorkedMinutes(workedMinutes);
        attendance.setPayableMinutes(payableMinutes);
        attendance.setLateMinutes(lateMinutes);
        attendance.setEarlyLeaveMinutes(earlyLeaveMinutes);
    }

    private Attendance findEntity(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return attendanceRepository.findById(id)
                .filter(attendance -> attendance.getEmployee().getStoreMember().getStore().getId().equals(storeId))
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id=" + id));
    }

    private Employee findCurrentEmployee() {
        String username = SecurityUtil.requireCurrentUserLogin();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username=" + username));
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(storeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found for user id=" + user.getId()));
    }

    private Office requireOffice(Employee employee) {
        Office office = employee.getOffice();
        if (office == null) {
            throw new BadRequestException(
                    "Employee id=" + employee.getId() + " has no office assigned");
        }
        return office;
    }

    private double calculateDistanceToOffice(double lat, double lng, Office office) {
        return haversineMetres(lat, lng,
                office.getOfficeLat().doubleValue(),
                office.getOfficeLng().doubleValue());
    }

    static double haversineMetres(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
