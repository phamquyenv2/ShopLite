package com.quyen.shoplite.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quyen.shoplite.domain.Attendance;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceMaintenanceService {

    private final AttendanceService attendanceService;

    @Scheduled(cron = "${shoplite.attendance.auto-close-cron:0 */15 * * * *}")
    @Transactional
    public void autoCloseMissingCheckOuts() {
        List<Attendance> openAttendances = attendanceService.findOpenAttendancesUpTo(attendanceService.currentDate());
        for (Attendance attendance : openAttendances) {
            LocalDateTime autoCloseAt = attendanceService.autoCheckoutTimeFor(attendance);
            if (attendanceService.currentDateTime().isBefore(autoCloseAt)) {
                continue;
            }
            attendanceService.autoCloseAttendance(attendance, autoCloseAt);
            log.info("Auto-closed attendance {}", attendance.getId());
        }
    }
}
