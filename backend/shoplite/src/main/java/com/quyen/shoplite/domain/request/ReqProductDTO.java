package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqProductDTO {

    @Positive(message = "categoryId must be greater than 0")
    @NotNull(message = "categoryId không được để trống")
    private Integer categoryId;

    @NotBlank(message = "name không được để trống")
    private String name;

    @NotBlank(message = "sku không được để trống")
    private String sku;

    @NotNull(message = "sellingPrice không được để trống")
    @Positive(message = "sellingPrice phải lớn hơn 0")
    @PositiveOrZero(message = "sellingPrice must be greater than or equal to 0")
    private Double sellingPrice;

    @NotNull(message = "costPrice không được để trống")
    @PositiveOrZero(message = "costPrice must be greater than or equal to 0")
    private Double costPrice;
}
