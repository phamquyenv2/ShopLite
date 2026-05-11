package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.UnitRepository;

import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqUnitUpsertDTO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UnitControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UnitRepository unitRepository;

    @Test
    @DisplayName("Create Unit - Success")
    void createUnit_Success() throws Exception {
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Kilogram");
        req.setDescription("Weight unit");

        mockMvc.perform(withStore(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Kilogram"));

        assertThat(unitRepository.existsByName("Kilogram")).isTrue();
    }

    @Test
    @DisplayName("Create Unit - Validation Failure (Blank Name)")
    void createUnit_ValidationFailure() throws Exception {
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName(""); 

        mockMvc.perform(withStore(post("/api/v1/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Get Unit - Success")
    void getUnit_Success() throws Exception {
        Unit unit = Unit.builder().store(testStore).name("Piece").build();
        unit = unitRepository.save(unit);

        mockMvc.perform(withStore(get("/api/v1/units/" + unit.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(unit.getId()))
                .andExpect(jsonPath("$.data.name").value("Piece"));
    }

    @Test
    @DisplayName("Update Unit - Success")
    void updateUnit_Success() throws Exception {
        Unit unit = Unit.builder().store(testStore).name("Old Name").build();
        unit = unitRepository.save(unit);

        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("New Name");

        mockMvc.perform(withStore(put("/api/v1/units/" + unit.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));

        Unit updated = unitRepository.findById(unit.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("Delete Unit - Success")
    void deleteUnit_Success() throws Exception {
        Unit unit = Unit.builder().store(testStore).name("To Delete").build();
        unit = unitRepository.save(unit);

        mockMvc.perform(withStore(delete("/api/v1/units/" + unit.getId())))
                .andExpect(status().isNoContent());

        assertThat(unitRepository.existsById(unit.getId())).isFalse();
    }
}
