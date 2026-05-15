package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ResOfficeDTO {
    private Integer id;
    private String name;
    private BigDecimal officeLat;
    private BigDecimal officeLng;
    private Integer radius;
}
