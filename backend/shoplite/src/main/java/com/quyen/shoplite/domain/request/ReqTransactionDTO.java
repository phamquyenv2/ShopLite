package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReqTransactionDTO {
    private String externalId;
    private String bankCode;

    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than 0")
    @PositiveOrZero(message = "amount must be greater than or equal to 0")
private Double amount;

    @NotNull(message = "type must not be null")
    private TypeTransactionEnum type;

    private String content;
    private LocalDateTime transactionTime;
    @Positive(message = "orderId must be greater than 0")
private Integer orderId;
}
