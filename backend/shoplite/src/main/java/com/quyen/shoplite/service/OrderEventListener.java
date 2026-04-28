package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.OrderCompletedEvent;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.PaymentStatusEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/**
 * Listens for {@link OrderCompletedEvent} and sends FCM push notifications
 * ONLY for bank-based payment methods (BANK_QR, BANK_TRANSFER, EWALLET).
 *
 * Rules enforced:
 *  1. Payment method must be a bank/digital method (not CASH)
 *  2. Payment status must be COMPLETED
 *  3. notificationSent must be false (idempotent — prevents duplicate sends on repeated webhooks)
 *
 * Runs AFTER_COMMIT so a notification is never sent for data that wasn't persisted.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final FcmService fcmService;
    private final PaymentRepository paymentRepository;

    /** Methods that trigger push notification */
    private static final Set<PaymentMethodEnum> BANK_METHODS = Set.of(
            PaymentMethodEnum.BANK_QR,
            PaymentMethodEnum.BANK_TRANSFER,
            PaymentMethodEnum.EWALLET
    );

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            var order = event.getOrder();

            // 1. Lookup payment via polymorphic reference
            Payment payment = paymentRepository.findByReferenceTypeAndReferenceId(
                    RefTypeEnum.ORDER, order.getId()).orElse(null);
            if (payment == null) {
                log.warn("[FCM] No payment found for order id={}. Notification skipped.", order.getId());
                return;
            }

            // 2. BANK-only rule
            if (!BANK_METHODS.contains(payment.getPaymentMethod())) {
                log.info("[FCM] Payment method {} is not a bank method. Skipping notification for order {}.",
                        payment.getPaymentMethod(), order.getCode());
                return;
            }

            // 3. Must be COMPLETED
            if (payment.getStatus() != PaymentStatusEnum.COMPLETED) {
                log.info("[FCM] Payment not yet COMPLETED (status={}). Skipping notification for order {}.",
                        payment.getStatus(), order.getCode());
                return;
            }

            // 4. Idempotent check — chống gửi trùng khi webhook gọi nhiều lần
            if (Boolean.TRUE.equals(payment.getNotificationSent())) {
                log.info("[FCM] Notification already sent for order {}. Skipping duplicate.", order.getCode());
                return;
            }

            // 5. Send
            fcmService.sendPaymentSuccessNotification(order);

            // 6. Mark as sent
            payment.setNotificationSent(true);
            paymentRepository.save(payment);

            log.info("[FCM] Notification sent and marked for order {}.", order.getCode());

        } catch (Exception e) {
            // Never let notification failure propagate — business transaction already committed
            log.error("[FCM] Lỗi khi gửi push notification cho đơn hàng {}: {}",
                    event.getOrder().getCode(), e.getMessage());
        }
    }
}
