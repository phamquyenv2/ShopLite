package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.UnitRepository;

import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqUnitUpsertDTO;

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
class UnitControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UnitRepository unitRepository;

    @Test
    @WithMockUser
    @DisplayName("Create Unit - Success")
    void createUnit_Success() throws Exception {
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Kilogram");
        req.setDescription("Weight unit");

        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Kilogram"));

        assertThat(unitRepository.existsByName("Kilogram")).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("Create Unit - Validation Failure (Blank Name)")
    void createUnit_ValidationFailure() throws Exception {
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName(""); 

        mockMvc.perform(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Get Unit - Success")
    void getUnit_Success() throws Exception {
        Unit unit = Unit.builder().name("Piece").build();
        unit = unitRepository.save(unit);

        mockMvc.perform(get("/api/v1/units/" + unit.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(unit.getId()))
                .andExpect(jsonPath("$.data.name").value("Piece"));
    }

    @Test
    @WithMockUser
    @DisplayName("Update Unit - Success")
    void updateUnit_Success() throws Exception {
        Unit unit = Unit.builder().name("Old Name").build();
        unit = unitRepository.save(unit);

        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("New Name");

        mockMvc.perform(put("/api/v1/units/" + unit.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));

        Unit updated = unitRepository.findById(unit.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    @WithMockUser
    @DisplayName("Delete Unit - Success")
    void deleteUnit_Success() throws Exception {
        Unit unit = Unit.builder().name("To Delete").build();
        unit = unitRepository.save(unit);

        mockMvc.perform(delete("/api/v1/units/" + unit.getId()))
                .andExpect(status().isNoContent());

        assertThat(unitRepository.existsById(unit.getId())).isFalse();
    }
}
