package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.DirectionEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResTransactionDTO {
    private Integer id;
    private TypeTransactionEnum type;
    private DirectionEnum direction;
    private BigDecimal amount;
    private String content;
    private String transactionCode;
    private LocalDateTime transactionTime;
    private LocalDateTime createdAt;

    // Payment reference
    private Integer paymentId;

    // FundAccount info
    private Integer fundAccountId;
    private String fundAccountName;

    // Balance snapshot
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
}
