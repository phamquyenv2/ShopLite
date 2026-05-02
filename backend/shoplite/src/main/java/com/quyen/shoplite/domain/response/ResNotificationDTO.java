package com.quyen.shoplite.domain.response;

import com.quyen.shoplite.util.constant.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ResNotificationDTO {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long referenceId;
    private boolean read;
    private boolean actionTaken;
    private LocalDateTime createdAt;
    private StoreInvitationInfo invitation;

    @Getter
    @Setter
    @Builder
    public static class StoreInvitationInfo {
        private Long id;
        private Long storeId;
        private String storeName;
        private String roleName;
        private String invitedByUsername;
        private String status;
        private LocalDateTime expiresAt;
    }
}
