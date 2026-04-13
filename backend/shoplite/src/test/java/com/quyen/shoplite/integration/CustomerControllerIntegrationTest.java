package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.Customer;
import com.quyen.shoplite.domain.request.ReqCustomerUpsertDTO;
import com.quyen.shoplite.repository.CustomerRepository;
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
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @WithMockUser
    @DisplayName("Create Customer - Success")
    void createCustomer_Success() throws Exception {
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Alice");
        req.setPhone("0987123456");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.phone").value("0987123456"));

        assertThat(customerRepository.existsByPhone("0987123456")).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("Create Customer - Invalid Phone Failure")
    void createCustomer_InvalidPhoneFailure() throws Exception {
        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Alice");
        req.setPhone("1234"); // Invalid vn format

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Create Customer - Duplicate Phone Failure")
    void createCustomer_DuplicatePhoneFailure() throws Exception {
        Customer existing = Customer.builder().name("Bob").phone("0987123456").build();
        customerRepository.save(existing);

        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("Alice");
        req.setPhone("0987123456"); // Existing phone

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Get Customer - Success")
    void getCustomer_Success() throws Exception {
        Customer customer = Customer.builder().name("Alice").phone("0987123456").build();
        customer = customerRepository.save(customer);

        mockMvc.perform(get("/api/v1/customers/" + customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alice"));
    }

    @Test
    @WithMockUser
    @DisplayName("Update Customer - Success")
    void updateCustomer_Success() throws Exception {
        Customer customer = Customer.builder().name("Old Name").phone("0987123456").build();
        customer = customerRepository.save(customer);

        ReqCustomerUpsertDTO req = new ReqCustomerUpsertDTO();
        req.setName("New Name");
        req.setPhone("0999999999");

        mockMvc.perform(put("/api/v1/customers/" + customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.phone").value("0999999999"));
    }

    @Test
    @WithMockUser
    @DisplayName("Delete Customer - Success")
    void deleteCustomer_Success() throws Exception {
        Customer customer = Customer.builder().name("To Delete").phone("0987123456").build();
        customer = customerRepository.save(customer);

        mockMvc.perform(delete("/api/v1/customers/" + customer.getId()))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.existsById(customer.getId())).isFalse();
    }
}
