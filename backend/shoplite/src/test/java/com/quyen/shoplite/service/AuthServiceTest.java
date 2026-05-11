package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.repository.UserTokenRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import com.quyen.shoplite.util.error.UnauthorizedException;

import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.UserToken;
import com.quyen.shoplite.domain.response.ResLoginDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StoreMemberRepository storeMemberRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private MenuService menuService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_success() {
        // Arrange
        String username = "testuser";
        String password = "password";

        Role role = new Role();
        role.setName("ADMIN");

        User user = new User();
        user.setId(1);
        user.setUsername(username);
        Store store = Store.builder().id(1L).name("Main Store").owner(user).build();
        StoreMember member = StoreMember.builder()
                .id(1L)
                .store(store)
                .user(user)
                .role(role)
                .status(StoreMemberStatus.ACTIVE)
                .build();

        lenient().when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(username, password));
        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        lenient().when(storeMemberRepository.findAllByUserIdAndStatusFetchStore(user.getId(), StoreMemberStatus.ACTIVE))
                .thenReturn(List.of(member));
        lenient().when(userTokenRepository.findValidTokensByUserId(eq(user.getId()), any())).thenReturn(List.of());

        lenient().when(securityUtil.generateAccessToken(username, "ADMIN")).thenReturn("access_token");
        lenient().when(securityUtil.generateRefreshToken(username)).thenReturn("refresh_token");
        lenient().when(securityUtil.getRefreshTokenExpiration()).thenReturn(3600L);

        // Act
        ResLoginDTO res = authService.login(username, password);

        // Assert
        assertNotNull(res);
        assertEquals("access_token", res.getAccessToken());
        assertEquals("refresh_token", res.getRefreshToken());

        ArgumentCaptor<UserToken> tokenCaptor = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository, times(1)).save(tokenCaptor.capture());

        UserToken savedToken = tokenCaptor.getValue();
        assertEquals("refresh_token", savedToken.getRefreshToken());
        assertFalse(savedToken.isRevoked());
        assertEquals(user, savedToken.getUser());
    }

    @Test
    void login_wrongPassword_throwsException() {
        // Arrange
        lenient().when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
            authService.login("testuser", "wrongpass");
        });
        assertTrue(ex.getMessage().contains("khong dung"));
    }

    @Test
    void login_missingUser_throwsException() {
        // Arrange
        String username = "missinguser";
        lenient().when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(username, "pass"));
        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
            authService.login(username, "pass");
        });
        assertTrue(ex.getMessage().contains("Khong tim thay user"));
    }

    @Test
    void refresh_success() {
        // Arrange
        Jwt refreshJwt = mock(Jwt.class);
        lenient().when(refreshJwt.getTokenValue()).thenReturn("old_refresh_token");
        lenient().when(refreshJwt.getSubject()).thenReturn("testuser");

        User user = new User();
        user.setId(1);
        user.setUsername("testuser");

        UserToken oldToken = new UserToken();
        oldToken.setRefreshToken("old_refresh_token");
        oldToken.setRevoked(false);
        oldToken.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        lenient().when(userTokenRepository.findByRefreshTokenAndRevokedFalse("old_refresh_token")).thenReturn(Optional.of(oldToken));
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        lenient().when(storeMemberRepository.findAllByUserIdAndStatusFetchStore(user.getId(), StoreMemberStatus.ACTIVE))
                .thenReturn(List.of());

        lenient().when(securityUtil.generateAccessToken("testuser", "USER")).thenReturn("new_access");
        lenient().when(securityUtil.generateRefreshToken("testuser")).thenReturn("new_refresh");
        lenient().when(securityUtil.getRefreshTokenExpiration()).thenReturn(3600L);

        // Act
        ResLoginDTO res = authService.refresh(refreshJwt);

        // Assert
        assertNotNull(res);
        assertEquals("new_access", res.getAccessToken());
        assertEquals("new_refresh", res.getRefreshToken());

        verify(userTokenRepository, times(2)).save(any(UserToken.class));
        assertTrue(oldToken.isRevoked()); // Verify old token is revoked
    }

    @Test
    void refresh_expiredTokenInDb_throwsExceptionAndRevokes() {
        // Arrange
        Jwt refreshJwt = mock(Jwt.class);
        lenient().when(refreshJwt.getTokenValue()).thenReturn("old_refresh_token");

        UserToken oldToken = new UserToken();
        oldToken.setRefreshToken("old_refresh_token");
        oldToken.setRevoked(false);
        oldToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        lenient().when(userTokenRepository.findByRefreshTokenAndRevokedFalse("old_refresh_token"))
                .thenReturn(Optional.of(oldToken));

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.refresh(refreshJwt));
        assertTrue(ex.getMessage().contains("het han"));

        assertTrue(oldToken.isRevoked());
        verify(userTokenRepository, times(1)).save(oldToken);
        verifyNoInteractions(userRepository);
        verify(securityUtil, never()).generateAccessToken(any(), any());
        verify(securityUtil, never()).generateRefreshToken(any());
    }

    @Test
    void refresh_revokedOrInvalidToken_throwsException() {
        // Arrange
        Jwt refreshJwt = mock(Jwt.class);
        lenient().when(refreshJwt.getTokenValue()).thenReturn("invalid_token");
        lenient().when(userTokenRepository.findByRefreshTokenAndRevokedFalse("invalid_token")).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
            authService.refresh(refreshJwt);
        });
        assertTrue(ex.getMessage().contains("thu hoi"));
    }

    @Test
    void logout_success() {
        // Arrange
        Jwt refreshJwt = mock(Jwt.class);
        lenient().when(refreshJwt.getTokenValue()).thenReturn("valid_token");

        UserToken oldToken = new UserToken();
        oldToken.setRefreshToken("valid_token");
        oldToken.setRevoked(false);

        lenient().when(userTokenRepository.findByRefreshTokenAndRevokedFalse("valid_token")).thenReturn(Optional.of(oldToken));

        // Act
        authService.logout(refreshJwt);

        // Assert
        assertTrue(oldToken.isRevoked());
        verify(userTokenRepository, times(1)).save(oldToken);
    }
}
