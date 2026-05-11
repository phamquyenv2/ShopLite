package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqEmployeeSalaryHistoryDTO;
import com.quyen.shoplite.domain.response.ResEmployeeSalaryHistoryDTO;
import com.quyen.shoplite.service.EmployeeSalaryHistoryService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmployeeSalaryHistoryController {

    private final EmployeeSalaryHistoryService salaryHistoryService;

    @PostMapping("/employees/{employeeId}/salary-histories")
    @ApiMessage("Create employee salary history success")
    public ResponseEntity<ResEmployeeSalaryHistoryDTO> create(
            @PathVariable("employeeId") @Positive(message = " must be greater than 0") Integer employeeId,
            @Valid @RequestBody ReqEmployeeSalaryHistoryDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaryHistoryService.createForEmployee(employeeId, req));
    }

    @GetMapping("/employees/{employeeId}/salary-histories")
    @ApiMessage("Get employee salary histories success")
    public ResponseEntity<List<ResEmployeeSalaryHistoryDTO>> findByEmployee(
            @PathVariable("employeeId") @Positive(message = " must be greater than 0") Integer employeeId) {
        return ResponseEntity.ok(salaryHistoryService.findByEmployee(employeeId));
    }

    @GetMapping("/employees/{employeeId}/salary-histories/current")
    @ApiMessage("Get current employee salary success")
    public ResponseEntity<ResEmployeeSalaryHistoryDTO> findCurrentByEmployee(
            @PathVariable("employeeId") @Positive(message = " must be greater than 0") Integer employeeId) {
        return ResponseEntity.ok(salaryHistoryService.findCurrentByEmployee(employeeId));
    }

    @GetMapping("/employee-salaries/me")
    @ApiMessage("Get my current salary success")
    public ResponseEntity<ResEmployeeSalaryHistoryDTO> findCurrentMine() {
        return ResponseEntity.ok(salaryHistoryService.findCurrentMine());
    }

    @GetMapping("/employee-salaries/me/history")
    @ApiMessage("Get my salary history success")
    public ResponseEntity<List<ResEmployeeSalaryHistoryDTO>> findMine() {
        return ResponseEntity.ok(salaryHistoryService.findMine());
    }
}
