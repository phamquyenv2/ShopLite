package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.ImportOrder;
import com.quyen.shoplite.domain.Notification;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.repository.NotificationRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.util.constant.NotificationType;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportOrderNotificationService {

    private final StoreMemberRepository storeMemberRepository;
    private final NotificationRepository notificationRepository;

    public void notifyWarehouseInspectionRequired(ImportOrder order, boolean reinspection) {
        String title = reinspection ? "Phiếu nhập cần kiểm tra lại" : "Có phiếu nhập cần kiểm tra";
        String message = code(order) + " - " + supplierName(order)
                + (reinspection ? " đã được yêu cầu kiểm tra lại" : " đang chờ nhân viên kho kiểm nhận");
        saveForRoles(order, List.of("WAREHOUSE"), NotificationType.IMPORT_ORDER_INSPECTION, title, message);
    }

    public void notifyManagerDiscrepancyApprovalRequired(ImportOrder order) {
        String message = code(order) + " - " + supplierName(order) + " có chênh lệch số lượng cần duyệt";
        saveForRoles(order, List.of("STORE_MANAGER"), NotificationType.IMPORT_ORDER_DISCREPANCY_APPROVAL,
                "Phiếu nhập cho duyệt chênh lệch", message);
    }

    private void saveForRoles(ImportOrder order, List<String> roleNames, NotificationType type,
                              String title, String message) {
        List<StoreMember> recipients = storeMemberRepository
                .findAllByStore_IdAndStatus(order.getStore().getId(), StoreMemberStatus.ACTIVE)
                .stream()
                .filter(member -> member.getRole() != null && roleNames.contains(member.getRole().getName()))
                .toList();
        LocalDateTime now = LocalDateTime.now();
        recipients.forEach(member -> notificationRepository.save(Notification.builder()
                .user(member.getUser())
                .type(type)
                .title(title)
                .message(message)
                .referenceId(order.getId().longValue())
                .read(false)
                .actionTaken(false)
                .createdAt(now)
                .build()));
    }

    private String code(ImportOrder order) {
        return "PN" + String.format("%06d", order.getId());
    }

    private String supplierName(ImportOrder order) {
        return order.getSupplier() != null ? order.getSupplier().getName() : "Nha cung cap";
    }
}
