package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqSupplierDTO;
import com.quyen.shoplite.domain.response.ResSupplierDTO;
import com.quyen.shoplite.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @ApiMessage("Create supplier success")
    public ResponseEntity<ResSupplierDTO> create(@Valid @RequestBody ReqSupplierDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get supplier success")
    public ResponseEntity<ResSupplierDTO> findById( @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get suppliers success")
    public ResponseEntity<List<ResSupplierDTO>> findAll() {
        return ResponseEntity.ok(supplierService.findAll());
    }

    @PutMapping("/{id}")
    @ApiMessage("Update supplier success")
    public ResponseEntity<ResSupplierDTO> update( @Positive(message = " must be greater than 0") Integer id, @Valid @RequestBody ReqSupplierDTO req) {
        return ResponseEntity.ok(supplierService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete supplier success")
    public ResponseEntity<Void> delete( @Positive(message = " must be greater than 0") Integer id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

