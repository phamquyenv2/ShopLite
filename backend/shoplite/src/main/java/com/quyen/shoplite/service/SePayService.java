package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * Tạo QR / link thanh toán SePay.
     * Chỉ cho phép trên đơn PENDING_PAYMENT.
     */
    public Map<String, Object> createPaymentSession(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + orderId));

        if (order.getStatus() != StatusEnum.PENDING_PAYMENT) {
            throw new IdInvalidException(
                    "Chỉ có thể tạo thanh toán cho đơn hàng ở trạng thái PENDING_PAYMENT (hiện tại: " + order.getStatus() + ")");
        }

        // Check if payment already exists
        if (paymentRepository.findByOrder_Id(orderId).isPresent()) {
            throw new IdInvalidException("Đơn hàng đã có giao dịch thanh toán");
        }

        try {
            String paymentUrl = createLinkToken(order);

            return Map.of(
                    "payment_url", paymentUrl,
                    "order_code", order.getCode(),
                    "amount", order.getTotalAmount()
            );
        } catch (Exception e) {
            log.error("Error creating SePay session for order {}", order.getCode(), e);
            throw new RuntimeException("Không thể tạo phiên thanh toán SePay", e);
        }
    }

    private String createLinkToken(Order order) {
        return "https://sepay.vn/pay/" + order.getCode() + "?amount=" + order.getTotalAmount();
    }

    /**
     * Xử lý webhook từ SePay khi khách thanh toán qua QR/chuyển khoản.
     * Delegate sang PaymentService để tạo Payment + Transaction + cập nhật Order.
     */
    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        log.info("Received SePay webhook: {}", payload);

        if (!payload.containsKey("transaction_id") || !payload.containsKey("amount") || !payload.containsKey("content")) {
            log.error("Invalid webhook payload missing critical fields");
            throw new IllegalArgumentException("Invalid webhook payload");
        }

        String transactionId = String.valueOf(payload.get("transaction_id"));
        Double amount = Double.valueOf(payload.get("amount").toString());
        String content = String.valueOf(payload.get("content"));

        // Extract order code from payment content
        String orderCode = extractOrderCode(content);
        if (orderCode == null || orderCode.isBlank()) {
            log.warn("Could not extract order code from content: {}", content);
            return;
        }

        // Find order with pessimistic lock
        Order order = orderRepository.findByCodeWithLock(orderCode).orElse(null);
        if (order == null) {
            log.error("Order not found for code: {}", orderCode);
            return;
        }

        // Idempotent: already completed → skip silently
        if (order.getStatus() == StatusEnum.COMPLETED) {
            log.info("Order {} is already COMPLETED. Webhook duplicate/delayed.", orderCode);
            return;
        }

        // Only process payment for PENDING_PAYMENT orders
        if (order.getStatus() != StatusEnum.PENDING_PAYMENT) {
            log.warn("Order {} is not in PENDING_PAYMENT state (status={}). Skipping webhook.", orderCode, order.getStatus());
            return;
        }

        // Amount validation
        if (Math.abs(order.getTotalAmount() - amount) > 0.1) {
            log.error("Amount mismatch for order {}. Expected: {}, Actual: {}", orderCode, order.getTotalAmount(), amount);
            return;
        }

        // Delegate to PaymentService — creates Payment + Transaction + updates Order + publishes event
        try {
            ReqPaymentDTO paymentReq = new ReqPaymentDTO();
            paymentReq.setMethod(PaymentMethodEnum.BANK);
            paymentReq.setAmount(amount);

            paymentService.createPayment(order.getId(), paymentReq);
            log.info("Successfully processed SePay webhook for order {} (txn={})", orderCode, transactionId);
        } catch (IdInvalidException e) {
            // Duplicate payment or other validation error — log and skip (idempotent)
            log.warn("[SePay] Payment creation skipped for order {}: {}", orderCode, e.getMessage());
        }
    }

    private String extractOrderCode(String content) {
        if (content == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("(ORD-[A-Z0-9]{8}|ORDER_\\w+|OD_\\w+)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return content.trim();
    }
}
