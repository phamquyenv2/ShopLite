package com.quyen.shoplite.repository;

import com.quyen.shoplite.util.constant.NotificationType;

import com.quyen.shoplite.domain.Notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUser_IdOrderByCreatedAtDesc(Integer userId);

    Optional<Notification> findByUser_IdAndId(Integer userId, Long id);

    Optional<Notification> findByUser_IdAndTypeAndReferenceId(Integer userId, NotificationType type, Long referenceId);
}
