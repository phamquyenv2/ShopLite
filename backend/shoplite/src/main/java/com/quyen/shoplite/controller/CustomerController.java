package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqCustomerUpsertDTO;
import com.quyen.shoplite.domain.response.ResCustomerDTO;
import com.quyen.shoplite.service.CustomerService;
import com.quyen.shoplite.util.annotation.ApiMessage;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ApiMessage("Create customer success")
    public ResponseEntity<ResCustomerDTO> create(@Valid @RequestBody ReqCustomerUpsertDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get customer success")
    public ResponseEntity<ResCustomerDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get customers success")
    public ResponseEntity<List<ResCustomerDTO>> findAll() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @PutMapping("/{id}")
    @ApiMessage("Update customer success")
    public ResponseEntity<ResCustomerDTO> update(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id, @Valid @RequestBody ReqCustomerUpsertDTO req) {
        return ResponseEntity.ok(customerService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete customer success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


