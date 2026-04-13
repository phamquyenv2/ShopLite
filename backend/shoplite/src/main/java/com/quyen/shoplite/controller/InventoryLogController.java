package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqInventoryLogDTO;
import com.quyen.shoplite.domain.response.ResInventoryLogDTO;
import com.quyen.shoplite.service.InventoryLogService;
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
@RequestMapping("/api/v1/inventory-logs")
@RequiredArgsConstructor
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    @PostMapping
    @ApiMessage("Create inventory log success")
    public ResponseEntity<ResInventoryLogDTO> create(@Valid @RequestBody ReqInventoryLogDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryLogService.create(req));
    }

    @GetMapping
    @ApiMessage("Get inventory logs success")
    public ResponseEntity<List<ResInventoryLogDTO>> findAll() {
        return ResponseEntity.ok(inventoryLogService.findAll());
    }

    @GetMapping("/product/{productId}")
    @ApiMessage("Get product inventory logs success")
    public ResponseEntity<List<ResInventoryLogDTO>> findByProductId( @Positive(message = " must be greater than 0") Integer productId) {
        return ResponseEntity.ok(inventoryLogService.findByProductId(productId));
    }
}

