package com.quyen.shoplite.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqImportOrderDTO {

    @NotNull(message = "supplierId không được để trống")
    private Integer supplierId;

    @NotEmpty(message = "items không được để trống")
    @Valid
    private List<ReqImportItemDTO> items;

    @PositiveOrZero(message = "tax phải >= 0")
    private Double tax;

    @PositiveOrZero(message = "discount phải >= 0")
    private Double discount;

    private String note;
}