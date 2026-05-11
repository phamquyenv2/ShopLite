package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.quyen.shoplite.domain.UserToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {

    Optional<UserToken> findByRefreshTokenAndRevokedFalse(String refreshToken);

    List<UserToken> findByUser_IdAndRevokedFalse(Integer userId);

    List<UserToken> findByUser_Id(Integer userId);

    /**
     * Tìm refresh token hợp lệ (chưa bị revoke và chưa hết hạn) mới nhất của user.
     */
    @Query("SELECT t FROM UserToken t " +
           "WHERE t.user.id = :userId " +
           "  AND t.revoked = false " +
           "  AND t.expiresAt > :now " +
           "ORDER BY t.createdAt DESC")
    List<UserToken> findValidTokensByUserId(@Param("userId") Integer userId,
                                            @Param("now") LocalDateTime now);

    /**
     * Revoke toàn bộ token hợp lệ của user (dùng khi login tạo token mới
     * hoặc khi cần force-logout tất cả thiết bị).
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserToken t SET t.revoked = true " +
           "WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") Integer userId);
}
