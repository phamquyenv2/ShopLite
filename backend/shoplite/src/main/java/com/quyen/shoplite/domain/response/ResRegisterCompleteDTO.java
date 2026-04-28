package com.quyen.shoplite.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response sau khi đăng ký hoàn tất — mở rộng ResLoginDTO
 * với thông tin Store và Office mặc định.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResRegisterCompleteDTO {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;
    private StoreInfo currentStore;
    private OfficeInfo currentOffice;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private Integer id;
        private String username;
        private String phone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String role;   // "OWNER"
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class OfficeInfo {
        private Integer id;
        private String name;
    }
}
