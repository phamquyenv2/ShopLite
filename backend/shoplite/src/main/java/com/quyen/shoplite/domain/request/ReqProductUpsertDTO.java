package com.quyen.shoplite.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqProductUpsertDTO {

    @NotNull(message = "categoryId must not be null")
    private Integer categoryId;

    @NotNull(message = "unitId must not be null")
    private Integer unitId;

    @NotBlank(message = "name must not be blank")
    @Size(max = 200, message = "name must be less than or equal to 200 characters")
    private String name;

    @Size(max = 100, message = "sku must be less than or equal to 100 characters")
    private String sku;

    @Min(value = 0, message = "barcode must be greater than or equal to 0")
    private Long barcode;

    @NotNull(message = "stock must not be null")
    @Min(value = 0, message = "stock must be greater than or equal to 0")
    private Integer stock;

    @NotNull(message = "price must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "price must be greater than or equal to 0")
    private Double price;

    private Integer version;
}
