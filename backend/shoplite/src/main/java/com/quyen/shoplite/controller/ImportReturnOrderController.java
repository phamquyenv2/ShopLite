package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.ImportReturnOrderService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqImportReturnOrderDTO;
import com.quyen.shoplite.domain.response.ResImportReturnOrderDTO;
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
@RequestMapping("/api/v1/import-return-orders")
@RequiredArgsConstructor
public class ImportReturnOrderController {

    private final ImportReturnOrderService importReturnOrderService;

    @PostMapping
    @ApiMessage("Create import return order success")
    public ResponseEntity<ResImportReturnOrderDTO> create(@Valid @RequestBody ReqImportReturnOrderDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importReturnOrderService.create(req));
    }

    @GetMapping
    @ApiMessage("Get import return orders success")
    public ResponseEntity<List<ResImportReturnOrderDTO>> findAll() {
        return ResponseEntity.ok(importReturnOrderService.findAll());
    }

    @GetMapping("/{id}")
    @ApiMessage("Get import return order success")
    public ResponseEntity<ResImportReturnOrderDTO> findById(
            @PathVariable("id") @Positive(message = "id phải > 0") Integer id) {
        return ResponseEntity.ok(importReturnOrderService.findById(id));
    }
}
