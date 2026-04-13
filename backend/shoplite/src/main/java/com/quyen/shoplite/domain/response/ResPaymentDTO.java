package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResPaymentDTO {
    private Integer id;
    private Integer orderId;
    private String orderCode;
    private PaymentMethodEnum method;
    private Double amount;
    private StatusEnum status;
    private java.time.LocalDateTime createdAt;
}
