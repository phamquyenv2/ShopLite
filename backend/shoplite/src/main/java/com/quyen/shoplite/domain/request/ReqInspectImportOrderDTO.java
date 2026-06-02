package com.quyen.shoplite.domain.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReqInspectImportOrderDTO {

    @NotEmpty(message = "items khong duoc de trong")
    @Valid
    private List<ReqInspectImportItemDTO> items;

    private String note;
}
