package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.InvitationStatus;
import com.quyen.shoplite.util.constant.NotificationType;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.PermissionException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqStoreInvitationDTO;
import com.quyen.shoplite.domain.response.ResMeDTO;
import com.quyen.shoplite.domain.response.ResPermissionDTO;
import com.quyen.shoplite.domain.response.ResStoreInvitationAcceptDTO;
import com.quyen.shoplite.domain.response.ResStoreInvitationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreInvitationService {

    private static final int INVITATION_EXPIRY_DAYS = 7;

    private final CurrentStoreService currentStoreService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final StoreInvitationRepository storeInvitationRepository;
    private final NotificationRepository notificationRepository;
    private final MenuService menuService;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;

    @Transactional
    public ResStoreInvitationDTO create(ReqStoreInvitationDTO req) {
        Store store = currentStoreService.getCurrentStore();
        User invitedBy = currentStoreService.getCurrentUser();
        String localPhone = normalizeLocalPhone(req.getPhone());

        User invitedUser = userRepository.findByPhone(localPhone)
                .or(() -> userRepository.findByPhone(RegistrationService.normalizeE164(localPhone)))
                .orElseThrow(() -> new BadRequestException("So dien thoai chua co tai khoan"));

        Role role = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id=" + req.getRoleId()));
        if (!role.isActive()) {
            throw new BadRequestException("Role is inactive");
        }

        if (storeMemberRepository.findByStoreIdAndUserId(store.getId(), invitedUser.getId()).isPresent()) {
            throw new BadRequestException("User is already a member of this store");
        }

        boolean pendingExists = storeInvitationRepository.existsByStore_IdAndInvitedUser_IdAndStatus(
                store.getId(), invitedUser.getId(), InvitationStatus.PENDING);
        if (pendingExists) {
            throw new BadRequestException("This user already has a pending invitation");
        }

        LocalDateTime now = LocalDateTime.now();
        StoreInvitation invitation = StoreInvitation.builder()
                .store(store)
                .invitedUser(invitedUser)
                .invitedBy(invitedBy)
                .role(role)
                .phone(localPhone)
                .status(InvitationStatus.PENDING)
                .expiresAt(now.plusDays(INVITATION_EXPIRY_DAYS))
                .createdAt(now)
                .build();
        invitation = storeInvitationRepository.save(invitation);

        notificationRepository.save(Notification.builder()
                .user(invitedUser)
                .type(NotificationType.STORE_INVITATION)
                .title("Lời mời vào cửa hàng")
                .message(invitedBy.getUsername() + " mời bạn tham gia " + store.getName() + " với vai trò " + role.getName())
                .referenceId(invitation.getId())
                .read(false)
                .actionTaken(false)
                .createdAt(now)
                .build());

        return toDto(invitation);
    }

    @Transactional
    public ResStoreInvitationAcceptDTO accept(Long invitationId) {
        User currentUser = currentStoreService.getCurrentUser();
        StoreInvitation invitation = storeInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id=" + invitationId));

        if (!invitation.getInvitedUser().getId().equals(currentUser.getId())) {
            throw new PermissionException("You cannot accept this invitation");
        }
        ensurePending(invitation);

        StoreMember member = storeMemberRepository
                .findByStoreIdAndUserId(invitation.getStore().getId(), currentUser.getId())
                .orElseGet(() -> storeMemberRepository.save(StoreMember.builder()
                        .store(invitation.getStore())
                        .user(currentUser)
                        .role(invitation.getRole())
                        .status(StoreMemberStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build()));

        if (member.getStatus() != StoreMemberStatus.ACTIVE) {
            member.setStatus(StoreMemberStatus.ACTIVE);
            member.setRole(invitation.getRole());
            member.setJoinedAt(LocalDateTime.now());
        }
        ensureEmployee(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        markNotificationHandled(currentUser.getId(), invitation.getId());

        ResMeDTO.StoreInfo currentStore = buildStoreInfo(member);
        return ResStoreInvitationAcceptDTO.builder()
                .currentStore(currentStore)
                .permissions(currentStore.getPermissions())
                .build();
    }

    @Transactional
    public ResStoreInvitationDTO decline(Long invitationId) {
        User currentUser = currentStoreService.getCurrentUser();
        StoreInvitation invitation = storeInvitationRepository.findByIdAndInvitedUser_Id(invitationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id=" + invitationId));
        ensurePending(invitation);

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        markNotificationHandled(currentUser.getId(), invitation.getId());
        return toDto(invitation);
    }

    private void ensurePending(StoreInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is not pending");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitation.setRespondedAt(LocalDateTime.now());
            markNotificationHandled(invitation.getInvitedUser().getId(), invitation.getId());
            throw new BadRequestException("Invitation expired");
        }
    }

    private void markNotificationHandled(Integer userId, Long invitationId) {
        notificationRepository
                .findByUser_IdAndTypeAndReferenceId(userId, NotificationType.STORE_INVITATION, invitationId)
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notification.setActionTaken(true);
                });
    }

    private ResMeDTO.StoreInfo buildStoreInfo(StoreMember member) {
        Role role = member.getRole();
        List<ResPermissionDTO> permissions = role == null ? List.of() : role.getPermissions().stream()
                .map(DTOMapper::toResPermissionDTO)
                .toList();
        return ResMeDTO.StoreInfo.builder()
                .id(member.getStore().getId())
                .name(member.getStore().getName())
                .memberRole(role != null ? role.getName() : "USER")
                .membershipStatus(member.getStatus().name())
                .permissions(permissions)
                .menus(menuService.getVisibleMenus(role))
                .build();
    }

    private void ensureEmployee(StoreMember member) {
        employeeRepository.findByStoreMember_Id(member.getId())
                .ifPresentOrElse(employee -> {
                    if (employee.isDeleted()) {
                        employee.setDeleted(false);
                        employeeRepository.save(employee);
                    }
                }, () -> {
                    Office defaultOffice = officeRepository.findAllByStoreIdOrderByIdAsc(member.getStore().getId()).stream()
                            .findFirst()
                            .orElse(null);
                    employeeRepository.save(Employee.builder()
                            .storeMember(member)
                            .store(member.getStore())
                            .office(defaultOffice)
                            .salaryRate(0.0)
                            .build());
                });
    }

    private ResStoreInvitationDTO toDto(StoreInvitation invitation) {
        return ResStoreInvitationDTO.builder()
                .id(invitation.getId())
                .storeId(invitation.getStore().getId())
                .storeName(invitation.getStore().getName())
                .invitedUserId(invitation.getInvitedUser().getId())
                .invitedUsername(invitation.getInvitedUser().getUsername())
                .phone(invitation.getPhone())
                .roleId(invitation.getRole().getId())
                .roleName(invitation.getRole().getName())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }

    private String normalizeLocalPhone(String rawPhone) {
        return RegistrationService.toLocalPhone(RegistrationService.normalizeE164(rawPhone));
    }
}
