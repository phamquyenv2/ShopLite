package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.AttendanceRepository;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Attendance;
import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckInDTO;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckOutDTO;
import com.quyen.shoplite.domain.response.ResAttendanceDTO;
import com.quyen.shoplite.domain.response.ResRosterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final long CHECK_IN_EARLY_WINDOW_MINUTES = 30;
    private static final long CHECK_OUT_EARLY_WINDOW_MINUTES = 30;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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

        Roster roster = req.getRosterId() != null
                ? findSelectedRosterForCheckIn(employee, req.getRosterId(), today, now.toLocalTime())
                : findRosterForCheckIn(employee, today, now.toLocalTime());
        if (attendanceRepository.existsByRoster_Id(roster.getId())) {
            throw new BadRequestException("Nhan vien chi duoc cham cong 1 lan trong 1 ca");
        }

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .office(office)
                .roster(roster)
                .checkIn(now)
                .workingDay(today)
                .walkIn(false)
                .latitude(BigDecimal.valueOf(req.getLatitude()))
                .longitude(BigDecimal.valueOf(req.getLongitude()))
                .distance(distanceMetres)
                .status(status)
                .build();

        attendanceRepository.save(attendance);

        if (status == AttendanceStatusEnum.OUT_OF_ZONE) {
            throw new BadRequestException("OUT_OF_ZONE: Bạn đang ở ngoài vùng cho phép chấm công (cách " +
                    String.format("%.0f", distanceMetres) + "m, giới hạn " + office.getRadius() + "m)");
        }

        return DTOMapper.toResAttendanceDTO(attendance);
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
        validateCheckOutWindow(attendance, checkOutTime);

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

    public List<ResRosterDTO> getTodayRostersForCurrentUser() {
        Employee employee = findCurrentEmployee();
        Long storeId = employee.getStoreMember().getStore().getId();
        LocalTime nowTime = LocalDateTime.now(applicationClock).toLocalTime();
        return rosterRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(
                        storeId, employee.getId(), LocalDate.now(applicationClock))
                .stream()
                .map(roster -> {
                    ResRosterDTO dto = DTOMapper.toResRosterDTO(roster);
                    if (dto != null && roster.getEndTime() != null) {
                        dto.setExpired(!nowTime.isBefore(checkInAllowedTo(roster)));
                    }
                    return dto;
                })
                .toList();
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
        LocalDateTime limitDT = attendance.getWorkingDay().atTime(LocalTime.of(23, 59));

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
        if (roster != null && roster.getStartTime() != null && roster.getEndTime() != null) {
            LocalDate     day          = attendance.getWorkingDay();
            LocalDateTime shiftStartDT = day.atTime(roster.getStartTime());
            LocalDateTime shiftEndDT   = day.atTime(roster.getEndTime());

            if (!roster.getEndTime().isAfter(roster.getStartTime())) {
                shiftEndDT = shiftEndDT.plusDays(1);
            }

            LocalDateTime graceDeadline = shiftStartDT;
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

    private Roster findSelectedRosterForCheckIn(Employee employee, Integer rosterId, LocalDate day, LocalTime nowTime) {
        Long storeId = employee.getStoreMember().getStore().getId();
        Roster roster = rosterRepository.findById(rosterId)
                .filter(item -> item.getEmployee().getId().equals(employee.getId()))
                .filter(item -> item.getEmployee().getStoreMember().getStore().getId().equals(storeId))
                .filter(item -> item.getWorkingDay().equals(day))
                .orElseThrow(() -> new ResourceNotFoundException("Roster not found for current employee"));

        if (roster.getStartTime() == null || roster.getEndTime() == null || !isWithinCheckInWindow(roster, nowTime)) {
            throw new BadRequestException(checkInWindowMessage(List.of(roster)));
        }
        return roster;
    }

    private Roster findRosterForCheckIn(Employee employee, LocalDate day, LocalTime nowTime) {
        Long storeId = employee.getStoreMember().getStore().getId();
        List<Roster> sameDayRosters = rosterRepository
                .findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(storeId, employee.getId(), day);

        return sameDayRosters.stream()
                .filter(roster -> roster.getStartTime() != null && roster.getEndTime() != null)
                .filter(roster -> isWithinCheckInWindow(roster, nowTime))
                .sorted(Comparator
                        .comparing((Roster roster) -> !isActiveShift(roster, nowTime))
                        .thenComparing(roster -> Math.abs(Duration.between(roster.getStartTime(), nowTime).toMinutes())))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(checkInWindowMessage(sameDayRosters)));
    }

    private boolean isWithinCheckInWindow(Roster roster, LocalTime nowTime) {
        LocalTime earliestCheckIn = checkInAllowedFrom(roster);
        LocalTime latestCheckIn = checkInAllowedTo(roster);
        return !nowTime.isBefore(earliestCheckIn) && nowTime.isBefore(latestCheckIn);
    }

    private boolean isActiveShift(Roster roster, LocalTime nowTime) {
        return !nowTime.isBefore(roster.getStartTime()) && nowTime.isBefore(roster.getEndTime());
    }

    private String checkInWindowMessage(List<Roster> rosters) {
        List<Roster> workingRosters = rosters.stream()
                .filter(roster -> roster.getStartTime() != null && roster.getEndTime() != null)
                .sorted(Comparator.comparing(Roster::getStartTime))
                .toList();

        if (workingRosters.isEmpty()) {
            return "Hom nay chua co ca lam hop le de check-in";
        }

        String windows = workingRosters.stream()
                .map(roster -> "tu " + formatTime(checkInAllowedFrom(roster))
                        + " den " + formatTime(checkInAllowedTo(roster))
                        + " (ca " + formatTime(roster.getStartTime()) + "-" + formatTime(roster.getEndTime()) + ")")
                .reduce((left, right) -> left + "; " + right)
                .orElse("");

        return "Chi duoc check-in trong khung gio " + windows;
    }

    private String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }

    private LocalTime checkInAllowedFrom(Roster roster) {
        return roster.getCheckInAllowedFrom() != null
                ? roster.getCheckInAllowedFrom()
                : roster.getStartTime().minusMinutes(CHECK_IN_EARLY_WINDOW_MINUTES);
    }

    private LocalTime checkInAllowedTo(Roster roster) {
        return roster.getCheckInAllowedTo() != null
                ? roster.getCheckInAllowedTo()
                : roster.getEndTime();
    }

    private void validateCheckOutWindow(Attendance attendance, LocalDateTime checkOutTime) {
        Roster roster = attendance.getRoster();
        if (roster == null || roster.getEndTime() == null) {
            return;
        }

        if (roster.getCheckOutAllowedFrom() != null || roster.getCheckOutAllowedTo() != null) {
            LocalDateTime allowedFrom = attendance.getWorkingDay().atTime(checkOutAllowedFrom(roster));
            LocalDateTime allowedTo = attendance.getWorkingDay().atTime(checkOutAllowedTo(roster));
            if (checkOutTime.isBefore(allowedFrom) || !checkOutTime.isBefore(allowedTo)) {
                throw new BadRequestException("Chi duoc check-out tu "
                        + formatTime(checkOutAllowedFrom(roster)) + " den " + formatTime(checkOutAllowedTo(roster)));
            }
            return;
        }

        LocalDateTime earliestCheckOut = attendance.getWorkingDay()
                .atTime(roster.getEndTime())
                .minusMinutes(CHECK_OUT_EARLY_WINDOW_MINUTES);

        if (checkOutTime.isBefore(earliestCheckOut)) {
            findRosterForCheckOut(attendance, checkOutTime)
                    .ifPresent(attendance::setRoster);
        }

        roster = attendance.getRoster();
        earliestCheckOut = attendance.getWorkingDay()
                .atTime(roster.getEndTime())
                .minusMinutes(CHECK_OUT_EARLY_WINDOW_MINUTES);

        if (checkOutTime.isBefore(earliestCheckOut)) {
            throw new BadRequestException("Chỉ được check-in trong vòng 30p trước giờ ra ca");
        }
    }

    private Optional<Roster> findRosterForCheckOut(Attendance attendance, LocalDateTime checkOutTime) {
        Employee employee = attendance.getEmployee();
        Long storeId = employee.getStoreMember().getStore().getId();
        LocalTime checkInTime = attendance.getCheckIn().toLocalTime();
        LocalTime checkOutLocalTime = checkOutTime.toLocalTime();

        return rosterRepository.findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(
                        storeId, employee.getId(), attendance.getWorkingDay())
                .stream()
                .filter(roster -> roster.getStartTime() != null && roster.getEndTime() != null)
                .filter(roster -> isWithinCheckInWindow(roster, checkInTime))
                .filter(roster -> !checkOutLocalTime.isBefore(checkOutAllowedFrom(roster)))
                .filter(roster -> checkOutLocalTime.isBefore(checkOutAllowedTo(roster)))
                .min(Comparator.comparing(roster -> Math.abs(Duration.between(roster.getEndTime(), checkOutLocalTime).toMinutes())));
    }

    private LocalTime checkOutAllowedFrom(Roster roster) {
        return roster.getCheckOutAllowedFrom() != null
                ? roster.getCheckOutAllowedFrom()
                : roster.getEndTime().minusMinutes(CHECK_OUT_EARLY_WINDOW_MINUTES);
    }

    private LocalTime checkOutAllowedTo(Roster roster) {
        return roster.getCheckOutAllowedTo() != null
                ? roster.getCheckOutAllowedTo()
                : roster.getEndTime();
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
