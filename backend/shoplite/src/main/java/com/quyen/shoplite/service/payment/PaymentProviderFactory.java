package com.quyen.shoplite.service.payment;

import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final SepayProvider sepayProvider;
    private final MomoProvider momoProvider;

    /**
     * Trả về PaymentProvider tương ứng.
     * Trả về null cho CASH / BANK_TRANSFER (thanh toán thủ công, không cần cổng).
     */
    public PaymentProvider getProvider(PaymentMethodEnum method) {
        if (method == null) {
            return null;
        }

        return switch (method) {
            case BANK_QR -> sepayProvider;
            case EWALLET -> momoProvider;
            case CASH, BANK_TRANSFER -> null; // Thanh toán thủ công
        };
    }
}
