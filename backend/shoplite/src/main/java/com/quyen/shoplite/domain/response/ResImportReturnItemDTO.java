package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResImportReturnItemDTO {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productSku;
    private String productImage;
    private Integer quantity;
    private Double returnPrice;
    private Double subTotal;
}
