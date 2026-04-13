package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqImportOrderDTO;
import com.quyen.shoplite.domain.request.ReqUpdateImportOrderStatusDTO;
import com.quyen.shoplite.domain.response.ResImportOrderDTO;
import com.quyen.shoplite.service.ImportOrderService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/import-orders")
@RequiredArgsConstructor
public class ImportOrderController {

    private final ImportOrderService importOrderService;

    @PostMapping
    @ApiMessage("Create import order success")
    public ResponseEntity<ResImportOrderDTO> create(@Valid @RequestBody ReqImportOrderDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importOrderService.create(req));
    }

    @GetMapping
    @ApiMessage("Get import orders success")
    public ResponseEntity<List<ResImportOrderDTO>> findAll() {
        return ResponseEntity.ok(importOrderService.findAll());
    }

    @GetMapping("/{id}")
    @ApiMessage("Get import order success")
    public ResponseEntity<ResImportOrderDTO> findById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(importOrderService.findById(id));
    }

    @PutMapping("/{id}/status")
    @ApiMessage("Update import order status success")
    public ResponseEntity<ResImportOrderDTO> updateStatus(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ReqUpdateImportOrderStatusDTO req) {
        return ResponseEntity.ok(importOrderService.updateStatus(id, req.getStatus()));
    }
}
