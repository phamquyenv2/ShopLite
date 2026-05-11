package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.CustomerRepository;

import com.quyen.shoplite.domain.Customer;
import com.quyen.shoplite.domain.request.ReqCustomerUpsertDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Create Customer - Success")
    void createCustomer_Success() throws Exception {
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Alice");
        req.setPhone("0987123456");

        mockMvc.perform(withStore(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.phone").value("0987123456"));

        assertThat(customerRepository.existsByPhone("0987123456")).isTrue();
    }

    @Test
    @DisplayName("Create Customer - Invalid Phone Failure")
    void createCustomer_InvalidPhoneFailure() throws Exception {
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Alice");
        req.setPhone("1234"); // Invalid vn format

        mockMvc.perform(withStore(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Create Customer - Duplicate Phone Failure")
    void createCustomer_DuplicatePhoneFailure() throws Exception {
        Customer existing = Customer.builder().store(testStore).name("Bob").phone("0987123456").build();
        customerRepository.save(existing);

        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Alice");
        req.setPhone("0987123456"); // Existing phone

        mockMvc.perform(withStore(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Get Customer - Success")
    void getCustomer_Success() throws Exception {
        Customer customer = Customer.builder().store(testStore).name("Alice").phone("0987123456").build();
        customer = customerRepository.save(customer);

        mockMvc.perform(withStore(get("/api/v1/customers/" + customer.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice"));
    }

    @Test
    @DisplayName("Update Customer - Success")
    void updateCustomer_Success() throws Exception {
        Customer customer = Customer.builder().store(testStore).name("Old Name").phone("0987123456").build();
        customer = customerRepository.save(customer);

        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("New Name");
        req.setPhone("0999999999");

        mockMvc.perform(withStore(put("/api/v1/customers/" + customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.phone").value("0999999999"));
    }

    @Test
    @DisplayName("Delete Customer - Success")
    void deleteCustomer_Success() throws Exception {
        Customer customer = Customer.builder().store(testStore).name("To Delete").phone("0987123456").build();
        customer = customerRepository.save(customer);

        mockMvc.perform(withStore(delete("/api/v1/customers/" + customer.getId())))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.existsById(customer.getId())).isFalse();
    }
}
