package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReqPaymentDTO {

    @NotNull(message = "referenceType không được để trống")
    private RefTypeEnum referenceType;

    @NotNull(message = "referenceId không được để trống")
    @Positive(message = "referenceId phải lớn hơn 0")
    private Integer referenceId;

    @NotNull(message = "paymentMethod không được để trống")
    private PaymentMethodEnum paymentMethod;

    @NotNull(message = "amount không được để trống")
    @Positive(message = "amount phải lớn hơn 0")
    private BigDecimal amount;

    /**
     * ID quỹ đích (optional) — nếu không truyền, backend sẽ tự resolve
     * theo loại phương thức thanh toán (CASH→CASH, BANK*→BANK, EWALLET→EWALLET).
     */
    @Positive(message = "fundAccountId phải lớn hơn 0")
    private Integer fundAccountId;

    private String createdBy;
}
