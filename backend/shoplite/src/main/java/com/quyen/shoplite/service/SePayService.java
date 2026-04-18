package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.Transaction;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.TransactionRepository;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final FcmService fcmService;

    public Map<String, Object> createPaymentSession(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getStatus() != StatusEnum.PENDING) {
            throw new IllegalStateException("Order is not in PENDING state. Current status: " + order.getStatus());
        }

        try {
            // Mock returning the hosted URL
            String paymentUrl = createLinkToken(order);

            return Map.of(
                    "payment_url", paymentUrl,
                    "order_code", order.getCode()
            );

        } catch (Exception e) {
            log.error("Error creating SePay session for order {}", order.getCode(), e);
            throw new RuntimeException("Could not create payment session via SePay", e);
        }
    }

    private String createLinkToken(Order order) {
        return "https://sepay.vn/pay/" + order.getCode() + "?amount=" + order.getTotalAmount();
    }

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

        // Note: idempotency pre-check removed (no externalId field). Consider checking by content+orderId.
        // Step 3: Extract order_code
        String orderCode = extractOrderCode(content);
        if (orderCode == null || orderCode.isBlank()) {
            log.warn("Could not extract order code from content: {}", content);
            return;
        }

        // Step 4: Validate Order
        Order order = orderRepository.findByCode(orderCode).orElse(null);
        if (order == null) {
            log.error("Order not found for code: {}", orderCode);
            // We should still log the transaction if it's orphaned, or just return. User said "log error".
            return;
        }

        if (order.getStatus() == StatusEnum.COMPLETED) {
            log.info("Order {} is already PAID. Webhook duplicate/delayed.", orderCode);
            return;
        }

        if (Math.abs(order.getTotalAmount() - amount) > 0.1) {
            log.error("Amount mismatch for order {}. Expected: {}, Actual: {}", orderCode, order.getTotalAmount(), amount);
            // Log it but do not process as paid. 
            // In a real system you might set to PARTIAL_PAID or similar, but here we just exit.
            return;
        }

        // Step 5: Save transaction and update order (Transactional ensures both succeed or fail together)
        Transaction transaction = Transaction.builder()
                .amount(amount)
                .type(TypeTransactionEnum.REVENUE)
                .content(content)
                .transactionTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .order(order)
                .build();

        try {
            // Idempotency Step 2: Database UNIQUE constraint enforcement
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            log.info("DuplicateKeyException caught: Webhook transaction {} already processed.", transactionId);
            return;
        }

        order.setStatus(StatusEnum.COMPLETED);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Successfully updated order {} to PAID state from SePay webhook.", orderCode);

        // ✅ Step 6: Push notification tới điện thoại người dùng
        try {
            fcmService.sendPaymentSuccessNotification(order);
        } catch (Exception e) {
            // Không để lỗi push notification làm hỏng transaction chính
            log.error("[FCM] Failed to send push notification for order {}: {}", orderCode, e.getMessage());
        }
    }

    private String extractOrderCode(String content) {
        // Regex to extract order codes like ORDER_123 or similar structures. 
        // We will assume "ORDER_" followed by numbers or characters.
        if (content == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("(ORDER_\\w+|OD_\\w+|[A-Z0-9]{8,15})");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Fallback to exact match logic if Regex fails but content is fairly clean
        return content.trim();
    }
}
