package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link OrderCompletedEvent} and sends FCM push notifications
 * only AFTER the originating database transaction has committed.
 *
 * This prevents the scenario where a notification is sent but the
 * corresponding DB changes are rolled back.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            fcmService.sendPaymentSuccessNotification(event.getOrder());
        } catch (Exception e) {
            // Never let notification failure propagate — business transaction already committed
            log.error("[FCM] Lỗi khi gửi push notification cho đơn hàng {}: {}",
                    event.getOrder().getCode(), e.getMessage());
        }
    }
}
