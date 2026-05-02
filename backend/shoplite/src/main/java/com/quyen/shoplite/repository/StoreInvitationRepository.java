package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.StoreInvitation;
import com.quyen.shoplite.util.constant.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreInvitationRepository extends JpaRepository<StoreInvitation, Long> {

    boolean existsByStore_IdAndInvitedUser_IdAndStatus(Long storeId, Integer invitedUserId, InvitationStatus status);

    Optional<StoreInvitation> findByIdAndInvitedUser_Id(Long id, Integer invitedUserId);
}
