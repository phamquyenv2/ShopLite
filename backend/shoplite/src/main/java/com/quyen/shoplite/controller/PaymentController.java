package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.response.ResPaymentDTO;
import com.quyen.shoplite.service.PaymentService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Tạo Payment session (polymorphic — hỗ trợ ORDER, IMPORT_ORDER, PAYROLL, SUPPLIER_RETURN, MANUAL).
     */
    @PostMapping
    @ApiMessage("Create payment session success")
    public ResponseEntity<ResPaymentDTO> createPaymentSession(@Valid @RequestBody ReqPaymentDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPaymentSession(req));
    }

    /**
     * Webhook SePay endpoint
     */
    @PostMapping("/webhook/sepay")
    @ApiMessage("Process SePay webhook success")
    public ResponseEntity<Void> handleSePayWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> payload) {
        try {
            paymentService.processWebhook(PaymentMethodEnum.BANK_QR, headers, payload);
        } catch (Exception e) {
            log.error("Error processing SePay webhook: ", e);
            if ("Invalid webhook signature".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook MoMo endpoint (IPN)
     */
    @PostMapping("/webhook/momo")
    @ApiMessage("Process MoMo webhook success")
    public ResponseEntity<Void> handleMoMoWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> payload) {
        try {
            paymentService.processWebhook(PaymentMethodEnum.EWALLET, headers, payload);
        } catch (Exception e) {
            log.error("Error processing MoMo webhook: ", e);
            if ("Invalid webhook signature".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        // MoMo expects 204 No Content for successful webhook acknowledgment
        return ResponseEntity.noContent().build();
    }

    /**
     * API kiểm tra trạng thái thanh toán theo reference (polymorphic).
     * Ví dụ: GET /api/v1/payment/status?referenceType=ORDER&referenceId=123
     */
    @GetMapping("/status")
    @ApiMessage("Get payment status success")
    public ResponseEntity<ResPaymentDTO> getPaymentStatus(
            @RequestParam RefTypeEnum referenceType,
            @RequestParam Integer referenceId) {
        try {
            return ResponseEntity.ok(paymentService.findByReference(referenceType, referenceId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Backward compatible — query by orderId.
     */
    @GetMapping("/orders/{orderId}/status")
    @ApiMessage("Get payment status success")
    public ResponseEntity<ResPaymentDTO> getPaymentStatusByOrderId(@PathVariable("orderId") Integer orderId) {
        try {
            return ResponseEntity.ok(paymentService.findByReference(RefTypeEnum.ORDER, orderId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
