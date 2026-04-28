package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.DirectionEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReqTransactionDTO {

    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "type must not be null")
    private TypeTransactionEnum type;

    @NotNull(message = "direction must not be null")
    private DirectionEnum direction;

    @NotNull(message = "fundAccountId must not be null")
    @Positive(message = "fundAccountId must be greater than 0")
    private Integer fundAccountId;

    private String content;
    private LocalDateTime transactionTime;

    /** Optional: link to a Payment */
    private Integer paymentId;
}
