package com.quyen.shoplite.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResSalesInitDTO {
    private List<ResProductDTO> products;
    private List<ResCategoryDTO> categories;
}
