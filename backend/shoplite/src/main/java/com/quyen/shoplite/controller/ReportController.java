package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.response.report.ResEndOfDayReportDTO;
import com.quyen.shoplite.domain.response.report.ResInventoryReportDTO;
import com.quyen.shoplite.domain.response.report.ResSalesReportDTO;
import com.quyen.shoplite.service.ReportService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/end-of-day")
    @ApiMessage("Get end of day report")
    public ResponseEntity<ResEndOfDayReportDTO> getEndOfDayReport(
            @RequestParam("storeId") Long storeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59, 999999999);
        return ResponseEntity.ok(reportService.getEndOfDayReport(storeId, from, to));
    }

    @GetMapping("/sales")
    @ApiMessage("Get sales report")
    public ResponseEntity<ResSalesReportDTO> getSalesReport(
            @RequestParam("storeId") Long storeId,
            @RequestParam(value = "period", defaultValue = "custom") String period,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59, 999999999);
        return ResponseEntity.ok(reportService.getSalesReport(storeId, period, from, to));
    }

    @GetMapping("/inventory")
    @ApiMessage("Get inventory report")
    public ResponseEntity<ResInventoryReportDTO> getInventoryReport(
            @RequestParam("storeId") Long storeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59, 999999999);
        return ResponseEntity.ok(reportService.getInventoryReport(storeId, from, to));
    }
}
