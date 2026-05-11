package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.UserRepository;

import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqUserDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("create user success")
    void createUser_Success() throws Exception {
        ReqUserDTO req = new ReqUserDTO();
        req.setUsername("newuser123");
        req.setPassword("Password!123");
        req.setActive(true);

        mockMvc.perform(withStore(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser123"));

        assertThat(userRepository.existsByUsername("newuser123")).isTrue();
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("create user duplicate username failure")
    void createUser_DuplicateUsername_Failure() throws Exception {
        User user = User.builder()
                .username("duplicateuser")
                .password("encoded_pass")
                .isActive(true)
                .build();
        userRepository.save(user);

        ReqUserDTO req = new ReqUserDTO();
        req.setUsername("duplicateuser");
        req.setPassword("Password!123");
        req.setActive(true);

        mockMvc.perform(withStore(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("get user by id success")
    void getUserById_Success() throws Exception {
        User user = User.builder()
                .username("findme")
                .password("encoded_pass")
                .isActive(true)
                .build();
        user = userRepository.save(user);

        mockMvc.perform(withStore(get("/api/v1/users/" + user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.username").value("findme"));
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("get user by id not found")
    void getUserById_NotFound() throws Exception {
        mockMvc.perform(withStore(get("/api/v1/users/999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("update user success")
    void updateUser_Success() throws Exception {
        User user = User.builder()
                .username("toupdate")
                .password("encoded_pass")
                .isActive(true)
                .build();
        user = userRepository.save(user);

        ReqUserDTO req = new ReqUserDTO();
        req.setUsername("toupdate"); // Username usually unchanged or validated
        req.setActive(false);

        mockMvc.perform(withStore(put("/api/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("delete user success")
    void deleteUser_Success() throws Exception {
        User user = User.builder()
                .username("todelete")
                .password("encoded_pass")
                .isActive(true)
                .build();
        user = userRepository.save(user);

        mockMvc.perform(withStore(delete("/api/v1/users/" + user.getId())))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }
}
