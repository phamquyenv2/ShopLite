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
public class ResMeDTO {
    private UserInfo user;
    private StoreInfo currentStore;
    private List<StoreInfo> stores;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Integer id;
        private String username;
        private String phone;
        private String globalRole;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreInfo {
        private Long id;
        private String name;
        private String memberRole;
        private String membershipStatus;
        private List<ResPermissionDTO> permissions;
        private List<ResMenuDTO> menus;
    }
}
