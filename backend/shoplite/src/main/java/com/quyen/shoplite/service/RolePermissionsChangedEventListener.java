package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.util.constant.StoreMemberStatus;

import com.quyen.shoplite.domain.RolePermissionsChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RolePermissionsChangedEventListener {

    private final StoreMemberRepository storeMemberRepository;
    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRolePermissionsChanged(RolePermissionsChangedEvent event) {
        try {
            var members = storeMemberRepository.findAllByRoleIdAndStatusFetchUser(
                    event.roleId(),
                    StoreMemberStatus.ACTIVE
            );

            if (members.isEmpty()) {
                log.info("[FCM] No active members found for role id={}. Permissions notification skipped.", event.roleId());
                return;
            }

            members.forEach(member ->
                    fcmService.sendPermissionsChangedNotification(member.getUser(), event.roleId(), event.roleName()));
        } catch (Exception e) {
            log.error("[FCM] Failed to publish permissions changed event for role id={}: {}",
                    event.roleId(), e.getMessage());
        }
    }
}
