package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqAdjustmentItemDTO {

    
    @Positive(message = "productId must be greater than 0")
@NotNull(message = "productId không được để trống")
    private Integer productId;

    /** Số lượng thực tế đếm được */
    @NotNull(message = "actualQuantity không được để trống")
    @Min(value = 0, message = "actualQuantity không được âm")
    private Integer actualQuantity;
}

