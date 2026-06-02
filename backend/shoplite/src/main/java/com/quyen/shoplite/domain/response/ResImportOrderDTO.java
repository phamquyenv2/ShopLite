package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import com.quyen.shoplite.util.constant.ImportReturnStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ResImportOrderDTO {
    private Integer id;

    // --- Supplier info ---
    private Integer supplierId;
    private String supplierName;

    private Double tax;
    private Double discount;
    private Double totalAmount;
    private Double amountPaid;
    private ImportOrderStatusEnum status;
    private ImportReturnStatusEnum returnStatus;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime inspectedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime stockAppliedAt;
    private String inspectedBy;
    private String approvedBy;
    private String discrepancyNote;

    /** Chi tiết từng sản phẩm nhập */
    private List<ResImportItemDTO> items;
}
