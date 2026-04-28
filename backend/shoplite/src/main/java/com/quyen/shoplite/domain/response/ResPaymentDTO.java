package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.PaymentStatusEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResPaymentDTO {
    private Integer id;
    private RefTypeEnum referenceType;
    private Integer referenceId;
    private PaymentMethodEnum paymentMethod;
    private BigDecimal amount;
    private PaymentStatusEnum status;
    private String qrUrl;
    private String transferContent;
    private String provider;
    private String createdBy;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
