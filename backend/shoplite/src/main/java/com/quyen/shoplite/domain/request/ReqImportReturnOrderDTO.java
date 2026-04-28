package com.quyen.shoplite.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqImportReturnOrderDTO {

    @NotNull(message = "supplierId không được null")
    private Integer supplierId;

    private Integer importOrderId; // optional – link to original phiếu nhập

    @NotNull
    @NotEmpty(message = "Phải có ít nhất 1 sản phẩm")
    @Valid
    private List<ReqImportReturnItemDTO> items;

    private Double discount;
    private Double amountPaid; // NCC đã trả lại bao nhiêu

    private String note;
    private String createdByUsername;
    private String receivedByUsername;

    /** Phương thức thanh toán (CASH, BANK_TRANSFER, ...) */
    private String paymentMethod;
    /** ID quỹ nhận tiền trả lại từ NCC */
    private Integer fundAccountId;
}
