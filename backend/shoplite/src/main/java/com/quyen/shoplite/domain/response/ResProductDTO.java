package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.quyen.shoplite.util.constant.ProductStatus;

@Getter
@Setter
public class ResProductDTO {

    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private Integer unitId;
    private String unitName;
    private String name;
    private String sku;
    private String barcode;
    private Integer stock;
    private Double costPrice;
    private Double sellingPrice;
    private Integer minStock;
    private Integer maxStock;
    private String image;
    private ProductStatus status;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
