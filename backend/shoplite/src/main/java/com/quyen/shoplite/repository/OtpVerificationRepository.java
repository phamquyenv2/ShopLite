package com.quyen.shoplite.repository;

import com.quyen.shoplite.util.constant.OtpPurpose;

import com.quyen.shoplite.domain.OtpVerification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** Lấy OTP mới nhất theo phone + purpose (dùng để verify) */
    Optional<OtpVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(
            String phone, OtpPurpose purpose);

    /** Đếm số lần gửi trong khung thời gian (rate limit) */
    @Query("SELECT COUNT(o) FROM OtpVerification o " +
           "WHERE o.phone = :phone AND o.purpose = :purpose AND o.createdAt >= :since")
    int countSentSince(@Param("phone") String phone,
                       @Param("purpose") OtpPurpose purpose,
                       @Param("since") LocalDateTime since);

    /** Lấy OTP gần nhất (để kiểm tra cooldown 60s) */
    @Query("SELECT o FROM OtpVerification o " +
           "WHERE o.phone = :phone AND o.purpose = :purpose " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatest(@Param("phone") String phone,
                                         @Param("purpose") OtpPurpose purpose);
}
