package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResImportItemDTO {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private Integer returnedQuantity;
    private Double importPrice;
    private Double subTotal;
}
