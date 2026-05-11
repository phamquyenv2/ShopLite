package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.NotificationRepository;
import com.quyen.shoplite.repository.StoreInvitationRepository;
import com.quyen.shoplite.util.constant.NotificationType;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Notification;
import com.quyen.shoplite.domain.StoreInvitation;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.response.ResNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final CurrentStoreService currentStoreService;
    private final NotificationRepository notificationRepository;
    private final StoreInvitationRepository storeInvitationRepository;

    @Transactional(readOnly = true)
    public List<ResNotificationDTO> findMine() {
        User user = currentStoreService.getCurrentUser();
        return notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ResNotificationDTO markRead(Long id) {
        User user = currentStoreService.getCurrentUser();
        Notification notification = notificationRepository.findByUser_IdAndId(user.getId(), id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id=" + id));
        notification.setRead(true);
        return toDto(notification);
    }

    private ResNotificationDTO toDto(Notification notification) {
        ResNotificationDTO.StoreInvitationInfo invitationInfo = null;
        if (notification.getType() == NotificationType.STORE_INVITATION && notification.getReferenceId() != null) {
            invitationInfo = storeInvitationRepository.findById(notification.getReferenceId())
                    .map(this::toInvitationInfo)
                    .orElse(null);
        }

        return ResNotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .actionTaken(notification.isActionTaken())
                .createdAt(notification.getCreatedAt())
                .invitation(invitationInfo)
                .build();
    }

    private ResNotificationDTO.StoreInvitationInfo toInvitationInfo(StoreInvitation invitation) {
        return ResNotificationDTO.StoreInvitationInfo.builder()
                .id(invitation.getId())
                .storeId(invitation.getStore().getId())
                .storeName(invitation.getStore().getName())
                .roleName(invitation.getRole().getName())
                .invitedByUsername(invitation.getInvitedBy().getUsername())
                .status(invitation.getStatus().name())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }
}
