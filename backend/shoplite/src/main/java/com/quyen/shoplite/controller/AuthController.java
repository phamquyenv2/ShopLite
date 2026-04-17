package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqLoginDTO;
import com.quyen.shoplite.domain.request.ReqRegisterDTO;
import com.quyen.shoplite.domain.response.ResLoginDTO;
import com.quyen.shoplite.service.AuthService;
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
        if (refreshJwt == null) {
            throw new UnauthorizedException("Refresh token is invalid");
        }
        return ResponseEntity.ok(authService.refresh(refreshJwt));
    }

    @GetMapping("/me")
    @ApiMessage("Get current user success")
    public ResponseEntity<Object> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return ResponseEntity.ok(jwt.getSubject());
    }

    @PostMapping("/logout")
    @ApiMessage("Logout success")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt refreshJwt) {
        if (refreshJwt == null) {
            throw new UnauthorizedException("Refresh token is invalid");
        }
        authService.logout(refreshJwt);
        return ResponseEntity.noContent().build();
    }
}
