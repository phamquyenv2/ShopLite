package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private Long barcode;
    private Integer stock;
    private Double price;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private Integer version;
}
