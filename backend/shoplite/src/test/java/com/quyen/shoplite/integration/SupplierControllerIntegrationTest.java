package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.SupplierRepository;

import com.quyen.shoplite.domain.Supplier;
import com.quyen.shoplite.domain.request.ReqSupplierDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SupplierControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    @Test
    @DisplayName("Create Supplier - Success")
    void createSupplier_Success() throws Exception {
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies");
        req.setPhone("0987123456");
        req.setEmail("contact@global.com");

        mockMvc.perform(withStore(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Global Supplies"))
                .andExpect(jsonPath("$.data.email").value("contact@global.com"));

        assertThat(supplierRepository.existsByName("Global Supplies")).isTrue();
    }

    @Test
    @DisplayName("Create Supplier - Invalid Email Failure")
    void createSupplier_InvalidEmailFailure() throws Exception {
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies");
        req.setEmail("invalid-email");

        mockMvc.perform(withStore(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Create Supplier - Invalid Phone Failure")
    void createSupplier_InvalidPhoneFailure() throws Exception {
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies");
        req.setPhone("123");

        mockMvc.perform(withStore(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Get Supplier - Success")
    void getSupplier_Success() throws Exception {
        Supplier supplier = Supplier.builder().store(testStore).name("Global Supplies").build();
        supplier = supplierRepository.save(supplier);

        mockMvc.perform(withStore(get("/api/v1/suppliers/" + supplier.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Global Supplies"));
    }

    @Test
    @DisplayName("Update Supplier - Success")
    void updateSupplier_Success() throws Exception {
        Supplier supplier = Supplier.builder().store(testStore).name("Global Supplies").build();
        supplier = supplierRepository.save(supplier);

        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Global Supplies Updated");
        req.setPhone("0999999999");
        req.setEmail("new@global.com");

        mockMvc.perform(withStore(put("/api/v1/suppliers/" + supplier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Global Supplies Updated"))
                .andExpect(jsonPath("$.data.email").value("new@global.com"));
    }

    @Test
    @DisplayName("Delete Supplier - Success")
    void deleteSupplier_Success() throws Exception {
        Supplier supplier = Supplier.builder().store(testStore).name("To Delete").build();
        supplier = supplierRepository.save(supplier);

        mockMvc.perform(withStore(delete("/api/v1/suppliers/" + supplier.getId())))
                .andExpect(status().isNoContent());

        assertThat(supplierRepository.existsById(supplier.getId())).isFalse();
    }
}
