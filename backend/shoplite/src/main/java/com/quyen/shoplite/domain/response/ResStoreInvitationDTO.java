package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.InvitationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ResStoreInvitationDTO {
    private Long id;
    private Long storeId;
    private String storeName;
    private Integer invitedUserId;
    private String invitedUsername;
    private String phone;
    private Long roleId;
    private String roleName;
    private InvitationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
