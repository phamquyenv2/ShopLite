package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqPayrollSyncDTO;
import com.quyen.shoplite.domain.response.ResPayrollDTO;
import com.quyen.shoplite.service.PayrollService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/sync-monthly")
    @ApiMessage("Sync payroll success")
    public ResponseEntity<List<ResPayrollDTO>> syncMonthly(@Valid @RequestBody ReqPayrollSyncDTO req) {
        return ResponseEntity.ok(payrollService.syncMonthlyPayroll(req));
    }

    @GetMapping
    @ApiMessage("Get payrolls success")
    public ResponseEntity<List<ResPayrollDTO>> findAll() {
        return ResponseEntity.ok(payrollService.findAll());
    }

    @GetMapping("/employee/{employeeId}")
    @ApiMessage("Get employee payrolls success")
    public ResponseEntity<List<ResPayrollDTO>> findByEmployee(@PathVariable Integer employeeId) {
        return ResponseEntity.ok(payrollService.findByEmployee(employeeId));
    }
}
