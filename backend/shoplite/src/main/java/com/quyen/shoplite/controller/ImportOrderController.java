package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.ImportOrderService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqImportOrderDTO;
import com.quyen.shoplite.domain.request.ReqImportOrderDecisionDTO;
import com.quyen.shoplite.domain.request.ReqInspectImportOrderDTO;
import com.quyen.shoplite.domain.request.ReqUpdateImportOrderStatusDTO;
import com.quyen.shoplite.domain.response.ResImportOrderDTO;
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
    public ResponseEntity<ResImportOrderDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(importOrderService.findById(id));
    }

    @PutMapping("/{id}")
    @ApiMessage("Update import order success")
    public ResponseEntity<ResImportOrderDTO> update(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqImportOrderDTO req) {
        return ResponseEntity.ok(importOrderService.update(id, req));
    }

    @PutMapping("/{id}/status")
    @ApiMessage("Update import order status success")
    public ResponseEntity<ResImportOrderDTO> updateStatus(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqUpdateImportOrderStatusDTO req) {
        return ResponseEntity.ok(importOrderService.updateStatus(id, req.getStatus()));
    }

    @PostMapping("/{id}/send")
    @ApiMessage("Send import order to supplier success")
    public ResponseEntity<ResImportOrderDTO> sendToSupplier(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(importOrderService.sendToSupplier(id));
    }

    @PostMapping("/{id}/inspect")
    @ApiMessage("Inspect import order success")
    public ResponseEntity<ResImportOrderDTO> inspect(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqInspectImportOrderDTO req) {
        return ResponseEntity.ok(importOrderService.inspect(id, req));
    }

    @PostMapping("/{id}/approve-discrepancy")
    @ApiMessage("Approve import order discrepancy success")
    public ResponseEntity<ResImportOrderDTO> approveDiscrepancy(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @RequestBody(required = false) ReqImportOrderDecisionDTO req) {
        return ResponseEntity.ok(importOrderService.approveDiscrepancy(id, req));
    }

    @PostMapping("/{id}/reject-discrepancy")
    @ApiMessage("Reject import order discrepancy success")
    public ResponseEntity<ResImportOrderDTO> rejectDiscrepancy(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @RequestBody(required = false) ReqImportOrderDecisionDTO req) {
        return ResponseEntity.ok(importOrderService.rejectDiscrepancy(id, req));
    }

    /**
     * POST /api/v1/import-orders/{id}/pay
     * Retry payment cho đơn đang ở PENDING_PAYMENT.
     * Không thực hiện lại update/confirm, chỉ tạo Payment + Transaction.
     */
    @PostMapping("/{id}/pay")
    @ApiMessage("Pay import order success")
    public ResponseEntity<ResImportOrderDTO> payOnly(
            @PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqImportOrderDTO req) {
        return ResponseEntity.ok(importOrderService.payOnly(id, req));
    }
}
