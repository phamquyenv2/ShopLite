package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqInventoryAdjustmentDTO;
import com.quyen.shoplite.domain.response.ResInventoryAdjustmentDTO;
import com.quyen.shoplite.service.InventoryAdjustmentService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory-adjustments")
@RequiredArgsConstructor
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService inventoryAdjustmentService;

    @PostMapping
    @ApiMessage("Create inventory adjustment success")
    public ResponseEntity<ResInventoryAdjustmentDTO> create(
            @Valid @RequestBody ReqInventoryAdjustmentDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryAdjustmentService.create(req));
    }

    @GetMapping
    @ApiMessage("Get inventory adjustments success")
    public ResponseEntity<List<ResInventoryAdjustmentDTO>> findAll() {
        return ResponseEntity.ok(inventoryAdjustmentService.findAll());
    }

    @GetMapping("/{id}")
    @ApiMessage("Get inventory adjustment success")
    public ResponseEntity<ResInventoryAdjustmentDTO> findById(
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(inventoryAdjustmentService.findById(id));
    }
}
