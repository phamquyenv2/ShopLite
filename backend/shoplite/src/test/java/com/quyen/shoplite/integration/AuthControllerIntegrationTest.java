package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.repository.UserTokenRepository;

import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.UserToken;
import com.quyen.shoplite.domain.request.ReqLoginDTO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends IntegrationTestBase {

    private static final String TEST_PHONE = "0900000001";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.findByUsername("auth_test_user").ifPresentOrElse(user -> {
            user.setPhone(TEST_PHONE);
            user.setPassword(passwordEncoder.encode("Password123!"));
            user.setActive(true);
            userRepository.save(user);
        }, () -> {
            User user = User.builder()
                    .username("auth_test_user")
                    .phone(TEST_PHONE)
                    .password(passwordEncoder.encode("Password123!"))
                    .isActive(true)
                    .build();
            userRepository.save(user);
        });
    }

    @AfterEach
    void tearDown() {
        userRepository.findByUsername("auth_test_user").ifPresent(user -> {
            // Delete ALL tokens for this user first (FK constraint)
            List<UserToken> tokens = userTokenRepository.findByUser_Id(user.getId());
            userTokenRepository.deleteAll(tokens);
            userRepository.delete(user);
        });
    }

    @Test
    @DisplayName("login success")
    void login_Success() throws Exception {
        ReqLoginDTO req = new ReqLoginDTO();
        req.setPhone(TEST_PHONE);
        req.setPassword("Password123!");

        mockMvc.perform(withStore(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("login invalid password")
    void login_InvalidPassword() throws Exception {
        ReqLoginDTO req = new ReqLoginDTO();
        req.setPhone(TEST_PHONE);
        req.setPassword("WrongPassword!");

        mockMvc.perform(withStore(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    @DisplayName("login unknown username")
    void login_UnknownUsername() throws Exception {
        ReqLoginDTO req = new ReqLoginDTO();
        req.setPhone("0999999999");
        req.setPassword("Password123!");

        mockMvc.perform(withStore(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    @Test
    @DisplayName("refresh success")
    void refresh_Success() throws Exception {
        // 1. Login to get token
        ReqLoginDTO req = new ReqLoginDTO();
        req.setPhone(TEST_PHONE);
        req.setPassword("Password123!");

        MvcResult result = mockMvc.perform(withStore(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String refreshToken = JsonPath.read(responseBody, "$.data.refreshToken");

        // 2. Perform refresh
        mockMvc.perform(withStore(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("refresh expired token in DB returns 401")
    void refresh_ExpiredTokenInDb() throws Exception {
        // 1. Login to get refresh token
        ReqLoginDTO req = new ReqLoginDTO();
        req.setPhone(TEST_PHONE);
        req.setPassword("Password123!");

        MvcResult result = mockMvc.perform(withStore(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String refreshToken = JsonPath.read(responseBody, "$.data.refreshToken");

        // 2. Force token to be expired in DB
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        userTokenRepository.save(token);

        // 3. Perform refresh - expect 401
        mockMvc.perform(withStore(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh invalid token")
    void refresh_InvalidToken() throws Exception {
        mockMvc.perform(withStore(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer invalid.fake.token")))
                .andExpect(status().isUnauthorized()); // Spring Security will block it before reaching controller if totally invalid
    }

    @Test
    @DisplayName("logout success - token is revoked")
    void logout_Success() throws Exception {
        // 1. Login to get token
        ReqLoginDTO req = new ReqLoginDTO();
        req.setPhone(TEST_PHONE);
        req.setPassword("Password123!");

        MvcResult result = mockMvc.perform(withStore(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String refreshToken = JsonPath.read(responseBody, "$.data.refreshToken");

        // 2. Perform logout - expect 204
        mockMvc.perform(withStore(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + refreshToken)))
                .andExpect(status().isNoContent());

        // 3. Verify the token has been marked revoked in the DB
        assertThat(userTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)).isEmpty();
    }
}
