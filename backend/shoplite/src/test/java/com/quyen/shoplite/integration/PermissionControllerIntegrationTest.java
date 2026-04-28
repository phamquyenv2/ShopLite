package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.request.ReqPermissionDTO;
import com.quyen.shoplite.repository.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    @WithMockUser(username = "admin", roles = "STORE_MANAGER")
    @DisplayName("create permission success")
    void createPermission_Success() throws Exception {
        ReqPermissionDTO req = new ReqPermissionDTO();
        req.setName("Test Permission");
        req.setApiPath("/api/test");
        req.setMethod("POST");
        req.setModule("TEST");

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Permission"))
                .andExpect(jsonPath("$.data.apiPath").value("/api/test"));

        assertThat(permissionRepository.existsByModuleAndApiPathAndMethod("TEST", "/api/test", "POST")).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = "STORE_MANAGER")
    @DisplayName("get permission by id success")
    void getPermission_Success() throws Exception {
        Permission permission = Permission.builder()
                .name("Get Test")
                .apiPath("/api/get")
                .method("GET")
                .module("GET_TEST")
                .build();
        permission = permissionRepository.save(permission);

        mockMvc.perform(get("/api/v1/permissions/" + permission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(permission.getId()))
                .andExpect(jsonPath("$.data.name").value("Get Test"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "STORE_MANAGER")
    @DisplayName("update permission success")
    void updatePermission_Success() throws Exception {
        Permission permission = Permission.builder()
                .name("Old Perm")
                .apiPath("/api/old")
                .method("PUT")
                .module("PUT_TEST")
                .build();
        permission = permissionRepository.save(permission);

        ReqPermissionDTO req = new ReqPermissionDTO();
        req.setName("New Perm");
        req.setApiPath("/api/new");
        req.setMethod("PUT");
        req.setModule("PUT_TEST");

        mockMvc.perform(put("/api/v1/permissions/" + permission.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Perm"));

        Permission updated = permissionRepository.findById(permission.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Perm");
    }

    @Test
    @WithMockUser(username = "admin", roles = "STORE_MANAGER")
    @DisplayName("delete permission success")
    void deletePermission_Success() throws Exception {
        Permission permission = Permission.builder()
                .name("Delete Perm")
                .apiPath("/api/del")
                .method("DELETE")
                .module("DEL_TEST")
                .build();
        permission = permissionRepository.save(permission);

        mockMvc.perform(delete("/api/v1/permissions/" + permission.getId()))
                .andExpect(status().isNoContent());

        assertThat(permissionRepository.existsById(permission.getId())).isFalse();
    }
}
