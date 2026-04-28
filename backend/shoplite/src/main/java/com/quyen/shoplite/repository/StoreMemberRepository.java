package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long> {

    @Query("""
            select sm
            from StoreMember sm
            join fetch sm.store s
            where sm.user.id = :userId and sm.status = :status
            order by sm.joinedAt desc
            """)
    List<StoreMember> findAllByUserIdAndStatusFetchStore(
            @Param("userId") Integer userId,
            @Param("status") StoreMemberStatus status);

    Optional<StoreMember> findByStore_IdAndUser_Id(Long storeId, Integer userId);

    default Optional<StoreMember> findByStoreIdAndUserId(Long storeId, Integer userId) {
        return findByStore_IdAndUser_Id(storeId, userId);
    }
}
