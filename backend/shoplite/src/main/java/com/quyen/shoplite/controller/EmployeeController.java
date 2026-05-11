package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.EmployeeService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqEmployeeDTO;
import com.quyen.shoplite.domain.response.ResEmployeeDTO;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ApiMessage("Create employee success")
    public ResponseEntity<ResEmployeeDTO> create(@Valid @RequestBody ReqEmployeeDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(req));
    }

    @GetMapping
    @ApiMessage("Get employees success")
    public ResponseEntity<List<ResEmployeeDTO>> findAll() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    @GetMapping("/{id}")
    @ApiMessage("Get employee success")
    public ResponseEntity<ResEmployeeDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PutMapping("/{id}")
    @ApiMessage("Update employee success")
    public ResponseEntity<ResEmployeeDTO> update(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqEmployeeDTO req) {
        return ResponseEntity.ok(employeeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete employee success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


