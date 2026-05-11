package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.AttendanceService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqAttendanceCheckInDTO;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckOutDTO;
import com.quyen.shoplite.domain.response.ResAttendanceDTO;
import com.quyen.shoplite.domain.response.ResRosterDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * POST /api/v1/attendance/check-in
     * Employee checks in using the authenticated user and current GPS coordinates.
     */
    @PostMapping("/check-in")
    @ApiMessage("Check-in success")
    public ResponseEntity<ResAttendanceDTO> checkIn(@Valid @RequestBody ReqAttendanceCheckInDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.checkIn(req));
    }

    /**
     * POST /api/v1/attendance/check-out
     * Employee checks out; calculates hours worked, late minutes, and early leave.
     */
    @PostMapping("/check-out")
    @ApiMessage("Check-out success")
    public ResponseEntity<ResAttendanceDTO> checkOut(@Valid @RequestBody ReqAttendanceCheckOutDTO req) {
        return ResponseEntity.ok(attendanceService.checkOut(req));
    }

    @GetMapping("/me/today")
    @ApiMessage("Get my attendance today success")
    public ResponseEntity<ResAttendanceDTO> getMyTodayAttendance() {
        ResAttendanceDTO attendance = attendanceService.getTodayForCurrentUser();
        if (attendance == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/me/rosters/today")
    @ApiMessage("Get my roster today success")
    public ResponseEntity<List<ResRosterDTO>> getMyTodayRosters() {
        return ResponseEntity.ok(attendanceService.getTodayRostersForCurrentUser());
    }

    /**
     * GET /api/v1/attendance
     * Return all attendance records, newest first.
     */
    @GetMapping
    @ApiMessage("Get attendance list success")
    public ResponseEntity<List<ResAttendanceDTO>> findAll() {
        return ResponseEntity.ok(attendanceService.findAll());
    }

    /**
     * GET /api/v1/attendance/{id}
     * Return a single attendance record by ID.
     */
    @GetMapping("/{id}")
    @ApiMessage("Get attendance success")
    public ResponseEntity<ResAttendanceDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(attendanceService.findById(id));
    }
}


