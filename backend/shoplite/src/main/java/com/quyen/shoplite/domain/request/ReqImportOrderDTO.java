package com.quyen.shoplite.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqImportOrderDTO {

    
    @Positive(message = "supplierId must be greater than 0")
@NotNull(message = "supplierId không được để trống")
    private Integer supplierId;

    @NotEmpty(message = "items không được để trống")
    @Valid
    private List<ReqImportItemDTO> items;

    @PositiveOrZero(message = "tax phải >= 0")
    @PositiveOrZero(message = "tax must be greater than or equal to 0")
private Double tax;

    @PositiveOrZero(message = "discount phải >= 0")
    @PositiveOrZero(message = "discount must be greater than or equal to 0")
    private Double discount;

    private String note;

    @PositiveOrZero(message = "paidAmount phải >= 0")
    private Double paidAmount;

    private String paymentMethod;

    /** ID quỹ dùng để chi tiền nhập hàng */
    private Integer fundAccountId;

    private com.quyen.shoplite.util.constant.ImportOrderStatusEnum status;
}
