package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqRosterDTO;
import com.quyen.shoplite.domain.response.ResRosterDTO;
import com.quyen.shoplite.service.RosterService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/roster")
@RequiredArgsConstructor
public class RosterController {

    private final RosterService rosterService;

    /** POST /api/v1/roster — Tạo lịch làm cho một nhân viên một ngày */
    @PostMapping
    @ApiMessage("Create roster success")
    public ResponseEntity<ResRosterDTO> create(@Valid @RequestBody ReqRosterDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rosterService.create(req));
    }

    /** GET /api/v1/roster/{id} — Lấy một bản ghi roster theo ID */
    @GetMapping("/{id}")
    @ApiMessage("Get roster success")
    public ResponseEntity<ResRosterDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(rosterService.findById(id));
    }

    /**
     * GET /api/v1/roster/employee/{employeeId}?from=YYYY-MM-DD&to=YYYY-MM-DD
     * Lấy lịch của một nhân viên trong khoảng ngày.
     */
    @GetMapping("/employee/{employeeId}")
    @ApiMessage("Get roster by employee success")
    public ResponseEntity<List<ResRosterDTO>> findByEmployee(
            @PathVariable("employeeId") @Positive(message = " must be greater than 0") Integer employeeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(rosterService.findByEmployeeAndRange(employeeId, from, to));
    }

    /**
     * GET /api/v1/roster/day?date=YYYY-MM-DD
     * Lấy lịch tất cả nhân viên trong một ngày (daily overview).
     */
    @GetMapping("/day")
    @ApiMessage("Get daily roster success")
    public ResponseEntity<List<ResRosterDTO>> findByDay(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(rosterService.findByDay(date));
    }

    /**
     * GET /api/v1/roster/month?month=YYYY-MM
     * Lay lich tat ca nhan vien trong mot thang de ve calendar.
     */
    @GetMapping("/month")
    @ApiMessage("Get monthly roster success")
    public ResponseEntity<List<ResRosterDTO>> findByMonth(@RequestParam("month") String month) {
        YearMonth ym = YearMonth.parse(month);
        return ResponseEntity.ok(rosterService.findByRange(ym.atDay(1), ym.atEndOfMonth()));
    }

    /** PUT /api/v1/roster/{id} — Cập nhật lịch (ví dụ đổi type sang LEAVE_APPROVED) */
    @PutMapping("/{id}")
    @ApiMessage("Update roster success")
    public ResponseEntity<ResRosterDTO> update(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqRosterDTO req) {
        return ResponseEntity.ok(rosterService.update(id, req));
    }

    /** DELETE /api/v1/roster/{id} — Xóa một bản ghi lịch */
    @DeleteMapping("/{id}")
    @ApiMessage("Delete roster success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        rosterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


