package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResCustomerDTO {
    private Integer id;
    private Integer version;
    private String name;
    private String phone;
    private Integer points;
}
