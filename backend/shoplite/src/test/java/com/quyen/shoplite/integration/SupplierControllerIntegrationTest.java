package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.SupplierRepository;

import com.quyen.shoplite.domain.Supplier;
import com.quyen.shoplite.domain.request.ReqSupplierDTO;

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
class SupplierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    @WithMockUser
    @DisplayName("Create Supplier - Success")
    void createSupplier_Success() throws Exception {
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies");
        req.setPhone("0987123456");
        req.setEmail("contact@global.com");

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Global Supplies"))
                .andExpect(jsonPath("$.data.email").value("contact@global.com"));

        assertThat(supplierRepository.existsByName("Global Supplies")).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("Create Supplier - Invalid Email Failure")
    void createSupplier_InvalidEmailFailure() throws Exception {
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies");
        req.setEmail("invalid-email");

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Create Supplier - Invalid Phone Failure")
    void createSupplier_InvalidPhoneFailure() throws Exception {
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies");
        req.setPhone("123");

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Get Supplier - Success")
    void getSupplier_Success() throws Exception {
        Supplier supplier = Supplier.builder().name("Global Supplies").build();
        supplier = supplierRepository.save(supplier);

        mockMvc.perform(get("/api/v1/suppliers/" + supplier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Global Supplies"));
    }

    @Test
    @WithMockUser
    @DisplayName("Update Supplier - Success")
    void updateSupplier_Success() throws Exception {
        Supplier supplier = Supplier.builder().name("Global Supplies").build();
        supplier = supplierRepository.save(supplier);

        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies Updated");
        req.setPhone("0999999999");
        req.setEmail("new@global.com");

        mockMvc.perform(put("/api/v1/suppliers/" + supplier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Global Supplies Updated"))
                .andExpect(jsonPath("$.data.email").value("new@global.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Delete Supplier - Success")
    void deleteSupplier_Success() throws Exception {
        Supplier supplier = Supplier.builder().name("To Delete").build();
        supplier = supplierRepository.save(supplier);

        mockMvc.perform(delete("/api/v1/suppliers/" + supplier.getId()))
                .andExpect(status().isNoContent());

        assertThat(supplierRepository.existsById(supplier.getId())).isFalse();
    }
}
