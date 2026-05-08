package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.response.ResOtpSendDTO;
import com.quyen.shoplite.domain.response.ResOtpVerifyDTO;
import com.quyen.shoplite.domain.response.ResRegisterCompleteDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.*;
import com.quyen.shoplite.util.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Xử lý toàn bộ luồng đăng ký qua OTP:
 *  1. Gửi OTP (với rate-limit)
 *  2. Xác thực OTP → tạo RegistrationSession
 *  3. Lưu tên cửa hàng vào session
 *  4. Hoàn tất: tạo User + Store + StoreMember + Office (trong 1 transaction)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final OtpVerificationRepository  otpRepo;
    private final RegistrationSessionRepository sessionRepo;
    private final UserRepository     userRepo;
    private final StoreRepository    storeRepo;
    private final StoreMemberRepository storeMemberRepo;
    private final OfficeRepository   officeRepo;
    private final FundAccountRepository fundAccountRepo;
    private final RoleRepository     roleRepo;
    private final UserTokenRepository userTokenRepo;

    private final TwilioService      twilioService;
    private final FcmService         fcmService;
    private final PasswordEncoder    passwordEncoder;
    private final SecurityUtil       securityUtil;

    @Value("${shoplite.otp.expiry-seconds:300}")
    private int otpExpirySeconds;

    @Value("${shoplite.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${shoplite.otp.max-send-per-hour:5}")
    private int maxSendPerHour;

    @Value("${shoplite.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${shoplite.registration.session-expiry-seconds:900}")
    private int sessionExpirySeconds;

    private static final SecureRandom RANDOM = new SecureRandom();

    // =========================================================================
    // BƯỚC 1: Gửi OTP
    // =========================================================================

    @Transactional
    public ResOtpSendDTO sendOtp(String rawPhone) {
        return sendOtp(rawPhone, null);
    }

    @Transactional
    public ResOtpSendDTO sendOtp(String rawPhone, String fcmToken) {
        String phone = normalizeE164(rawPhone);
        String localPhone = toLocalPhone(phone);

        // 1. Kiểm tra phone đã tồn tại user chưa
        if (userRepo.existsByPhone(phone) || userRepo.existsByPhone(localPhone)) {
            throw new BadRequestException("Số điện thoại đã được đăng ký. Vui lòng đăng nhập.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. Rate limit: max 5 lần/giờ
        int sentInHour = otpRepo.countSentSince(phone, OtpPurpose.REGISTER,
                now.minusHours(1));
        if (sentInHour >= maxSendPerHour) {
            throw new BadRequestException(
                    "Bạn đã gửi quá nhiều OTP. Vui lòng thử lại sau 1 giờ.");
        }

        // 3. Cooldown: chờ ít nhất 60s giữa 2 lần gửi
        otpRepo.findLatest(phone, OtpPurpose.REGISTER).ifPresent(last -> {
            long secondsSinceLast = java.time.Duration.between(last.getCreatedAt(), now).getSeconds();
            if (secondsSinceLast < resendCooldownSeconds) {
                long wait = resendCooldownSeconds - secondsSinceLast;
                throw new BadRequestException(
                        "Vui lòng chờ " + wait + " giây trước khi gửi lại OTP.");
            }
        });

        // 4. Tạo OTP 6 số
        String rawOtp = String.format("%06d", RANDOM.nextInt(1_000_000));

        // 5. Hash OTP
        String otpHash = passwordEncoder.encode(rawOtp);

        // 6. Lưu vào DB
        OtpVerification otpVerification = OtpVerification.builder()
                .phone(phone)
                .otpHash(otpHash)
                .purpose(OtpPurpose.REGISTER)
                .expiresAt(now.plusSeconds(otpExpirySeconds))
                .maxAttempts(maxAttempts)
                .createdAt(now)
                .build();
        otpRepo.save(otpVerification);

        // 7. Gửi SMS
        if (fcmToken != null && !fcmToken.isBlank()) {
            fcmService.sendRegistrationOtpToToken(fcmToken.trim(), phone, rawOtp, otpExpirySeconds);
        } else {
            twilioService.sendOtp(phone, rawOtp);
        }

        return new ResOtpSendDTO("OTP sent", phone, otpExpirySeconds, resendCooldownSeconds);
    }

    // =========================================================================
    // BƯỚC 2: Xác thực OTP
    // =========================================================================

    @Transactional
    public ResOtpVerifyDTO verifyOtp(String rawPhone, String inputOtp) {
        String phone = normalizeE164(rawPhone);
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm OTP mới nhất
        OtpVerification otp = otpRepo
                .findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.REGISTER)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy OTP. Vui lòng gửi lại."));

        // 2. Kiểm tra hết hạn
        if (otp.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("OTP đã hết hạn. Vui lòng gửi lại.");
        }

        // 3. Kiểm tra đã verified
        if (otp.isVerified()) {
            throw new BadRequestException("OTP này đã được sử dụng. Vui lòng gửi lại.");
        }

        // 4. Kiểm tra số lần thử
        if (otp.getAttemptCount() >= otp.getMaxAttempts()) {
            throw new BadRequestException("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng gửi lại.");
        }

        // 5. So sánh OTP (BCrypt)
        if (!passwordEncoder.matches(inputOtp, otp.getOtpHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepo.save(otp);
            int remaining = otp.getMaxAttempts() - otp.getAttemptCount();
            throw new BadRequestException("OTP không đúng. Còn " + remaining + " lần thử.");
        }

        // 6. Đánh dấu verified
        otp.setVerified(true);
        otp.setVerifiedAt(now);
        otpRepo.save(otp);

        // 7. Tạo RegistrationSession
        String sessionId = "reg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        RegistrationSession session = RegistrationSession.builder()
                .id(sessionId)
                .phone(phone)
                .status(RegSessionStatus.OTP_VERIFIED)
                .expiresAt(now.plusSeconds(sessionExpirySeconds))
                .createdAt(now)
                .build();
        sessionRepo.save(session);

        return new ResOtpVerifyDTO(sessionId, phone, sessionExpirySeconds);
    }

    // =========================================================================
    // BƯỚC 3: Lưu tên cửa hàng
    // =========================================================================

    @Transactional
    public void setStoreName(String sessionId, String storeName) {
        RegistrationSession session = findValidSession(sessionId, RegSessionStatus.OTP_VERIFIED);
        session.setStoreName(storeName.trim());
        session.setStatus(RegSessionStatus.STORE_NAMED);
        sessionRepo.save(session);
    }

    // =========================================================================
    // BƯỚC 4: Hoàn tất đăng ký
    // =========================================================================

    @Transactional
    public ResRegisterCompleteDTO completeRegister(String sessionId, String password) {
        RegistrationSession session = findValidSession(sessionId, RegSessionStatus.STORE_NAMED);
        String phone = session.getPhone();
        String localPhone = toLocalPhone(phone);

        // Double-check phone chưa tồn tại
        if (userRepo.existsByPhone(phone) || userRepo.existsByPhone(localPhone)) {
            throw new BadRequestException("Số điện thoại đã được đăng ký. Vui lòng đăng nhập.");
        }

        // 1. Tạo User (username = phone E.164)
        Role storeManagerRole = roleRepo.findByName("STORE_MANAGER")
                .orElseThrow(() -> new BadRequestException("Role STORE_MANAGER chưa được khởi tạo"));

        User user = User.builder()
                .username(phone)
                .phone(localPhone)
                .password(passwordEncoder.encode(password))
                .isActive(true)
                .build();
        user = userRepo.save(user);

        // 2. Tạo Store
        Store store = Store.builder()
                .name(session.getStoreName())
                .owner(user)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        store = storeRepo.save(store);

        // 3. Tạo StoreMember (OWNER)
        StoreMember member = StoreMember.builder()
                .store(store)
                .user(user)
                .role(storeManagerRole)
                .status(StoreMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        storeMemberRepo.save(member);

        // 4. Tạo Office mặc định
        Office office = Office.builder()
                .name("Chi nhánh chính")
                .store(store)
                .build();
        office = officeRepo.save(office);

        // 5. Tạo 3 FundAccount mặc định cho store
        fundAccountRepo.save(FundAccount.builder()
                .store(store)
                .name("Tiền mặt tại quầy")
                .type(FundTypeEnum.CASH)
                .openingBalance(java.math.BigDecimal.ZERO)
                .balance(java.math.BigDecimal.ZERO)
                .build());
        fundAccountRepo.save(FundAccount.builder()
                .store(store)
                .name("Tài khoản ngân hàng")
                .type(FundTypeEnum.BANK)
                .openingBalance(java.math.BigDecimal.ZERO)
                .balance(java.math.BigDecimal.ZERO)
                .build());
        fundAccountRepo.save(FundAccount.builder()
                .store(store)
                .name("Ví điện tử")
                .type(FundTypeEnum.EWALLET)
                .openingBalance(java.math.BigDecimal.ZERO)
                .balance(java.math.BigDecimal.ZERO)
                .build());

        // 6. Mark session COMPLETED
        session.setStatus(RegSessionStatus.COMPLETED);
        sessionRepo.save(session);

        // 7. Issue JWT
        String roleName = storeManagerRole.getName();
        String accessToken  = securityUtil.generateAccessToken(user.getUsername(), roleName);
        String refreshToken = securityUtil.generateRefreshToken(user.getUsername());

        UserToken userToken = UserToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(securityUtil.getRefreshTokenExpiration()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        userTokenRepo.save(userToken);

        // 8. Build response
        ResRegisterCompleteDTO.UserInfo userInfo = new ResRegisterCompleteDTO.UserInfo(
                user.getId(), user.getUsername(), localPhone);
        ResRegisterCompleteDTO.StoreInfo storeInfo = new ResRegisterCompleteDTO.StoreInfo(
                store.getId(), store.getName(), storeManagerRole.getName());
        ResRegisterCompleteDTO.OfficeInfo officeInfo = new ResRegisterCompleteDTO.OfficeInfo(
                office.getId(), office.getName());

        return new ResRegisterCompleteDTO(accessToken, refreshToken, userInfo, storeInfo, officeInfo);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Chuẩn hóa số điện thoại về E.164.
     * 0912345678 → +84912345678
     */
    public static String normalizeE164(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");
        if (cleaned.startsWith("+84")) return cleaned;
        if (cleaned.startsWith("84"))  return "+" + cleaned;
        if (cleaned.startsWith("0"))   return "+84" + cleaned.substring(1);
        throw new BadRequestException("Số điện thoại không hợp lệ: " + phone);
    }

    public static String toLocalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");
        if (cleaned.startsWith("+84") && cleaned.length() == 12) return "0" + cleaned.substring(3);
        if (cleaned.startsWith("84") && cleaned.length() == 11) return "0" + cleaned.substring(2);
        if (cleaned.startsWith("0") && cleaned.length() == 10) return cleaned;
        throw new BadRequestException("Số điện thoại không hợp lệ: " + phone);
    }

    private RegistrationSession findValidSession(String sessionId, RegSessionStatus expectedStatus) {
        RegistrationSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Session đăng ký không tồn tại hoặc đã hết hạn."));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Phiên đăng ký đã hết hạn. Vui lòng bắt đầu lại.");
        }

        if (session.getStatus() != expectedStatus) {
            throw new BadRequestException("Trạng thái phiên đăng ký không hợp lệ: " + session.getStatus());
        }

        return session;
    }
}
