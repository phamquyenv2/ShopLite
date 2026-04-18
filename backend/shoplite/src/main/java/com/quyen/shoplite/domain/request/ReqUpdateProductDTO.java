package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO dùng để cập nhật sản phẩm (không cho phép đổi stock).
 */
@Getter
@Setter
public class ReqUpdateProductDTO {

    @Positive(message = "categoryId must be greater than 0")
    private Integer categoryId;
    private String name;

    @PositiveOrZero(message = "sellingPrice must be greater than or equal to 0")
    private Double sellingPrice;

    @PositiveOrZero(message = "costPrice must be greater than or equal to 0")
    private Double costPrice;
}
