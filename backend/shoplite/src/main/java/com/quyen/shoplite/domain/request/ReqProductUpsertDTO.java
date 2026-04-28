package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

import com.quyen.shoplite.util.constant.ProductStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqProductUpsertDTO {

    @Positive(message = "categoryId must be greater than 0")
    @NotNull(message = "categoryId must not be null")
    private Integer categoryId;

    @Positive(message = "unitId must be greater than 0")
    @NotNull(message = "unitId must not be null")
    private Integer unitId;

    @NotBlank(message = "name must not be blank")
    @Size(max = 200, message = "name must be less than or equal to 200 characters")
    private String name;

    @Size(max = 100, message = "sku must be less than or equal to 100 characters")
    private String sku;

    @Size(max = 100, message = "barcode must be less than or equal to 100 characters")
    private String barcode;

    @PositiveOrZero(message = "stock must be greater than or equal to 0")
    @NotNull(message = "stock must not be null")
    private Integer stock;

    @PositiveOrZero(message = "sellingPrice must be greater than or equal to 0")
    @NotNull(message = "sellingPrice must not be null")
    private Double sellingPrice;

    @PositiveOrZero(message = "costPrice must be greater than or equal to 0")
    @NotNull(message = "costPrice must not be null")
    private Double costPrice;

    @PositiveOrZero(message = "minStock must be greater than or equal to 0")
    private Integer minStock;

    @PositiveOrZero(message = "maxStock must be greater than or equal to 0")
    private Integer maxStock;

    @Size(max = 500, message = "image must be less than or equal to 500 characters")
    private String image;

    private ProductStatus status;

    private Integer version;
}
