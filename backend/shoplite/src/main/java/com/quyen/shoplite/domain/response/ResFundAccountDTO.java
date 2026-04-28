package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.FundTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResFundAccountDTO {
    private Integer id;
    private String name;
    private FundTypeEnum type;
    private BigDecimal balance;
    private BigDecimal openingBalance;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
