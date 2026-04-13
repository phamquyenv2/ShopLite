package com.quyen.shoplite.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqInventoryAdjustmentDTO {

    @NotBlank(message = "reason không được để trống")
    private String reason;

    private String note;

    /** Tên người thực hiện kiểm kê */
    @NotBlank(message = "createdBy không được để trống")
    private String createdBy;

    /** Danh sách sản phẩm điều chỉnh kèm số lượng thực đếm */
    @NotEmpty(message = "items không được để trống")
    @Valid
    private List<ReqAdjustmentItemDTO> items;
}
