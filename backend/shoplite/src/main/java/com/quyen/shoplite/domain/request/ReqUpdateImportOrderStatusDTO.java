package com.quyen.shoplite.domain.request;

import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqUpdateImportOrderStatusDTO {

    @NotNull(message = "status không được để trống")
    private ImportOrderStatusEnum status;
}
