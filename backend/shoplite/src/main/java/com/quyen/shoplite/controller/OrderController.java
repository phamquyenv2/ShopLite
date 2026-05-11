package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.OrderService;
import com.quyen.shoplite.service.PaymentService;
import com.quyen.shoplite.service.TransactionService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import com.quyen.shoplite.util.constant.StatusEnum;

import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.response.ResOrderDTO;
import com.quyen.shoplite.domain.response.ResPaymentDTO;
import com.quyen.shoplite.domain.response.ResTransactionDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final TransactionService transactionService;

    // ==================== ORDER CRUD ====================

    /**
     * POST /api/v1/orders — Tạo đơn DRAFT (không trừ kho)
     */
    @PostMapping
    @ApiMessage("Create draft order success")
    public ResponseEntity<ResOrderDTO> create(@Valid @RequestBody ReqOrderDTO req) {
        OrderService.CreateOrderResult result = orderService.create(req);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.order());
    }

    /**
     * PUT /api/v1/orders/{id} — Cập nhật đơn DRAFT (thêm/bớt item, đổi discount, customer)
     */
    @PutMapping("/{id}")
    @ApiMessage("Update draft order success")
    public ResponseEntity<ResOrderDTO> update(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id,
            @Valid @RequestBody ReqOrderDTO req) {
        return ResponseEntity.ok(orderService.update(id, req));
    }

    /**
     * PATCH /api/v1/orders/{id}/confirm — Chốt đơn, trừ kho → PENDING_PAYMENT
     */
    @PatchMapping("/{id}/confirm")
    @ApiMessage("Confirm order success")
    public ResponseEntity<ResOrderDTO> confirm(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id) {
        return ResponseEntity.ok(orderService.confirm(id));
    }

    /**
     * PATCH /api/v1/orders/{id}/status — Admin/backoffice thay đổi status
     */
    @PatchMapping("/{id}/status")
    @ApiMessage("Update order status success")
    public ResponseEntity<ResOrderDTO> updateStatus(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id,
            @RequestParam("status") StatusEnum status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    /**
     * DELETE /api/v1/orders/{id} — Huỷ đơn (hoàn kho nếu đã confirm)
     */
    @DeleteMapping("/{id}")
    @ApiMessage("Cancel order success")
    public ResponseEntity<Void> cancel(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id) {
        orderService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/{id}")
    @ApiMessage("Get order success")
    public ResponseEntity<ResOrderDTO> findById(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /**
     * GET /api/v1/orders
     */
    @GetMapping
    @ApiMessage("Get orders success")
    public ResponseEntity<List<ResOrderDTO>> findAll(
            @RequestParam(value = "statuses", required = false) List<StatusEnum> statuses,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        return ResponseEntity.ok(orderService.findAll(statuses, from, to));
    }

    // ==================== PAYMENT ====================

    /**
     * POST /api/v1/orders/{id}/payments — Thu tiền thủ công hoặc tạo session thanh toán QR
     */
    @PostMapping("/{id}/payments")
    @ApiMessage("Create payment session for order success")
    public ResponseEntity<ResPaymentDTO> createPaymentSession(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id,
            @RequestBody ReqPaymentDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPaymentSession(id, req));
    }

    /**
     * GET /api/v1/orders/{id}/payments — Xem payment của đơn
     */
    @GetMapping("/{id}/payments")
    @ApiMessage("Get payment for order success")
    public ResponseEntity<ResPaymentDTO> getPaymentByOrderId(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id) {
        return ResponseEntity.ok(paymentService.findByOrderId(id));
    }

    // ==================== TRANSACTIONS ====================

    /**
     * GET /api/v1/orders/{id}/transactions — Xem tiền đã thu / refund
     */
    @GetMapping("/{id}/transactions")
    @ApiMessage("Get transactions for order success")
    public ResponseEntity<List<ResTransactionDTO>> getTransactionsByOrderId(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Integer id) {
        // Lấy payment của order → lấy transactions theo paymentId
        ResPaymentDTO payment = paymentService.findByOrderId(id);
        if (payment == null || payment.getId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(transactionService.findByPaymentId(payment.getId()));
    }
}
