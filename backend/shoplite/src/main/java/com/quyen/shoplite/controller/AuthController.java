package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.*;
import com.quyen.shoplite.domain.response.*;
import com.quyen.shoplite.service.AuthService;
import com.quyen.shoplite.service.RegistrationService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import com.quyen.shoplite.util.error.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    // ─── Login / Logout / Refresh ─────────────────────────────────────────────

    @PostMapping("/login")
    @ApiMessage("Login success")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO req) {
        ResLoginDTO result = authService.login(req.getPhone(), req.getPassword());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    @ApiMessage("Register success")
    public ResponseEntity<ResLoginDTO> register(@Valid @RequestBody ReqRegisterDTO req) {
        ResLoginDTO result = authService.register(req.getUsername(), req.getPhone(), req.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/refresh")
    @ApiMessage("Refresh token success")
    public ResponseEntity<ResLoginDTO> refresh(@AuthenticationPrincipal Jwt refreshJwt) {
        if (refreshJwt == null) throw new UnauthorizedException("Refresh token is invalid");
        return ResponseEntity.ok(authService.refresh(refreshJwt));
    }

    @GetMapping("/me")
    @ApiMessage("Get current user success")
    public ResponseEntity<ResMeDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new UnauthorizedException("User is not authenticated");
        return ResponseEntity.ok(authService.getCurrentUserProfile(jwt.getSubject()));
    }

    @PostMapping("/logout")
    @ApiMessage("Logout success")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt refreshJwt) {
        if (refreshJwt == null) throw new UnauthorizedException("Refresh token is invalid");
        authService.logout(refreshJwt);
        return ResponseEntity.noContent().build();
    }

    // ─── OTP Registration Flow ────────────────────────────────────────────────

    /**
     * Bước 1: Gửi OTP qua SMS
     * POST /api/v1/auth/register/otp/send
     */
    @PostMapping("/register/otp/send")
    @ApiMessage("OTP sent")
    public ResponseEntity<ResOtpSendDTO> sendOtp(@Valid @RequestBody ReqOtpSendDTO req) {
        ResOtpSendDTO result = registrationService.sendOtp(req.getPhone());
        return ResponseEntity.ok(result);
    }

    /**
     * Bước 2: Xác thực OTP → trả registerSessionId
     * POST /api/v1/auth/register/otp/verify
     */
    @PostMapping("/register/otp/verify")
    @ApiMessage("OTP verified")
    public ResponseEntity<ResOtpVerifyDTO> verifyOtp(@Valid @RequestBody ReqOtpVerifyDTO req) {
        ResOtpVerifyDTO result = registrationService.verifyOtp(req.getPhone(), req.getOtp());
        return ResponseEntity.ok(result);
    }

    /**
     * Bước 3: Đặt tên cửa hàng
     * POST /api/v1/auth/register/store
     */
    @PostMapping("/register/store")
    @ApiMessage("Store name saved")
    public ResponseEntity<Void> setStoreName(
            @Valid @RequestBody ReqSetStoreNameDTO req) {
        registrationService.setStoreName(req.getRegisterSessionId(), req.getStoreName());
        return ResponseEntity.ok().build();
    }

    /**
     * Bước 4: Đặt mật khẩu + hoàn tất → trả JWT
     * POST /api/v1/auth/register/complete
     */
    @PostMapping("/register/complete")
    @ApiMessage("Registration completed")
    public ResponseEntity<ResRegisterCompleteDTO> completeRegister(
            @Valid @RequestBody ReqCompleteRegisterDTO req) {
        ResRegisterCompleteDTO result = registrationService.completeRegister(
                req.getRegisterSessionId(), req.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

