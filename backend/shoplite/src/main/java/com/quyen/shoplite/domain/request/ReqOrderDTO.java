package com.quyen.shoplite.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqOrderDTO {

    
    @Positive(message = "userId must be greater than 0")
@NotNull(message = "userId must not be null")
    private Integer userId;

    private String requestId;

    
    @Positive(message = "customerId must be greater than 0")
    private Integer customerId;

    
    @PositiveOrZero(message = "discount must be greater than or equal to 0")
private Double discount;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<ReqOrderItemDTO> items;
}
