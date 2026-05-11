package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.CategoryRepository;

import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.request.ReqCategoryUpsertDTO;

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
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @WithMockUser
    @DisplayName("Create Category - Success")
    void createCategory_Success() throws Exception {
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Electronics");

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Electronics"));

        assertThat(categoryRepository.existsByName("Electronics")).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("Create Category - Validation Failure (Blank Name)")
    void createCategory_ValidationFailure() throws Exception {
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName(""); // Blank

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Get Category - Success")
    void getCategory_Success() throws Exception {
        Category category = Category.builder().name("Books").build();
        category = categoryRepository.save(category);

        mockMvc.perform(get("/api/v1/categories/" + category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(category.getId()))
                .andExpect(jsonPath("$.data.name").value("Books"));
    }

    @Test
    @WithMockUser
    @DisplayName("Get Category - Not Found")
    void getCategory_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/categories/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Update Category - Success")
    void updateCategory_Success() throws Exception {
        Category category = Category.builder().name("Old Name").build();
        category = categoryRepository.save(category);

        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("New Name");

        mockMvc.perform(put("/api/v1/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));

        Category updated = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    @WithMockUser
    @DisplayName("Delete Category - Success")
    void deleteCategory_Success() throws Exception {
        Category category = Category.builder().name("To Delete").build();
        category = categoryRepository.save(category);

        mockMvc.perform(delete("/api/v1/categories/" + category.getId()))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.existsById(category.getId())).isFalse();
    }
}
