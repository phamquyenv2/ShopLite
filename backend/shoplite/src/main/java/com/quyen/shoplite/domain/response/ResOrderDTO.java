package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.StatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ResOrderDTO {
    private Integer id;
    private String code;
    private String requestId;
    private Integer customerId;
    private String customerName;
    private Double totalAmount;
    private Double discount;
    private StatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;
    private Integer userId;
    private String username;
    private List<ResOrderItemDTO> items;
}
