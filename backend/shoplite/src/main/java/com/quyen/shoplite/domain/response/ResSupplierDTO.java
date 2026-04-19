package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResSupplierDTO {
    private Integer id;
    private Integer version;
    private String name;
    private String phone;
    private String address;
    private String email;
    private LocalDateTime createdAt;
}
