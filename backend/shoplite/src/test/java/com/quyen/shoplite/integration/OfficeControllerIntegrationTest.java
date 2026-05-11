package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.OfficeRepository;

import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.request.ReqOfficeDTO;

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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OfficeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OfficeRepository officeRepository;

    @Test
    @WithMockUser
    @DisplayName("Create Office - Success")
    void createOffice_Success() throws Exception {
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Main Office");
        req.setOfficeLat(BigDecimal.valueOf(10.123));
        req.setOfficeLng(BigDecimal.valueOf(20.456));
        req.setRadius(500);

        mockMvc.perform(post("/api/v1/offices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Main Office"));

        assertThat(officeRepository.existsByName("Main Office")).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("Create Office - Invalid Location Data Failure")
    void createOffice_InvalidLocationDataFailure() throws Exception {
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Main Office");
        req.setOfficeLat(null); // Missing required field
        req.setOfficeLng(BigDecimal.valueOf(20.456));
        req.setRadius(-10); // Negative radius

        mockMvc.perform(post("/api/v1/offices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Get Office - Success")
    void getOffice_Success() throws Exception {
        Office office = Office.builder()
                .name("Main Office")
                .officeLat(BigDecimal.valueOf(10.123))
                .officeLng(BigDecimal.valueOf(20.456))
                .radius(500)
                .build();
        office = officeRepository.save(office);

        mockMvc.perform(get("/api/v1/offices/" + office.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Main Office"));
    }

    @Test
    @WithMockUser
    @DisplayName("Update Office - Success")
    void updateOffice_Success() throws Exception {
        Office office = Office.builder()
                .name("Main Office")
                .officeLat(BigDecimal.valueOf(10.123))
                .officeLng(BigDecimal.valueOf(20.456))
                .radius(500)
                .build();
        office = officeRepository.save(office);

        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Updated Office");
        req.setOfficeLat(BigDecimal.valueOf(11.111));
        req.setOfficeLng(BigDecimal.valueOf(22.222));
        req.setRadius(1000);

        mockMvc.perform(put("/api/v1/offices/" + office.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Office"))
                .andExpect(jsonPath("$.data.radius").value(1000));
    }

    @Test
    @WithMockUser
    @DisplayName("Delete Office - Success")
    void deleteOffice_Success() throws Exception {
        Office office = Office.builder()
                .name("To Delete")
                .officeLat(BigDecimal.valueOf(10.123))
                .officeLng(BigDecimal.valueOf(20.456))
                .radius(500)
                .build();
        office = officeRepository.save(office);

        mockMvc.perform(delete("/api/v1/offices/" + office.getId()))
                .andExpect(status().isNoContent());

        assertThat(officeRepository.existsById(office.getId())).isFalse();
    }
}
