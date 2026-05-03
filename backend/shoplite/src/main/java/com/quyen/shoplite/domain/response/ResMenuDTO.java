package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.MenuType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResMenuDTO {
    private Long id;
    private String code;
    private String title;
    private String route;
    private String icon;
    private MenuType menuType;
    private Long parentId;
    private Integer sortOrder;
}
