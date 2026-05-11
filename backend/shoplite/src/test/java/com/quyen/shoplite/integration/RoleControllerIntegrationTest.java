package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.RoleRepository;

import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.request.ReqRoleDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoleControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("create role success")
    void createRole_Success() throws Exception {
        ReqRoleDTO req = new ReqRoleDTO();
        req.setName("EDITOR");
        req.setDescription("Content Editor");
        req.setActive(true);
        req.setPermissionIds(List.of());

        mockMvc.perform(withStore(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("EDITOR"));

        assertThat(roleRepository.existsByName("EDITOR")).isTrue();
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("get role by id success")
    void getRole_Success() throws Exception {
        Role role = Role.builder()
                .name("VIEWER")
                .description("Just viewing")
                .active(true)
                .build();
        role = roleRepository.save(role);

        mockMvc.perform(withStore(get("/api/v1/roles/" + role.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(role.getId()))
                .andExpect(jsonPath("$.data.name").value("VIEWER"));
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("update role success")
    void updateRole_Success() throws Exception {
        Role role = Role.builder()
                .name("OLD_ROLE")
                .active(true)
                .build();
        role = roleRepository.save(role);

        ReqRoleDTO req = new ReqRoleDTO();
        req.setName("NEW_ROLE");
        req.setActive(false);

        mockMvc.perform(withStore(put("/api/v1/roles/" + role.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("NEW_ROLE"))
                .andExpect(jsonPath("$.data.active").value(false));

        Role updated = roleRepository.findById(role.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("NEW_ROLE");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @WithMockUser(username = IntegrationTestBase.TEST_USERNAME)
    @DisplayName("delete role success")
    void deleteRole_Success() throws Exception {
        Role role = Role.builder()
                .name("TO_DELETE")
                .active(true)
                .build();
        role = roleRepository.save(role);

        mockMvc.perform(withStore(delete("/api/v1/roles/" + role.getId())))
                .andExpect(status().isNoContent());

        assertThat(roleRepository.existsById(role.getId())).isFalse();
    }
}
