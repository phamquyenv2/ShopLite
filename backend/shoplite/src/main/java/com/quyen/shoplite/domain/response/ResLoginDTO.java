package com.quyen.shoplite.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response trả về sau khi đăng nhập thành công.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResLoginDTO {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;
    private StoreInfo currentStore;

    public ResLoginDTO(String accessToken, String refreshToken, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Integer id;
        private String username;
        private String roleName;    // Tên role (thay RoleEnum)
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String memberRole;
    }
}
