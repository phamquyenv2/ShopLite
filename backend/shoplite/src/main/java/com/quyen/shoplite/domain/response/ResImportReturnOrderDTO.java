package com.quyen.shoplite.domain.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ResImportReturnOrderDTO {
    private Integer id;

    // Supplier info
    private Integer supplierId;
    private String supplierName;
    private String supplierPhone;

    // Linked import order (optional)
    private Integer importOrderId;

    private Double totalAmount;
    private Double discount;
    private Double amountPaid; // NCC đã trả lại

    private String note;
    private String createdByUsername;
    private String receivedByUsername;
    private LocalDateTime createdAt;

    /** Chi tiết từng sản phẩm trả */
    private List<ResImportReturnItemDTO> items;
}
