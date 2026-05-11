package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.CategoryRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Category;
import com.quyen.shoplite.domain.request.ReqCategoryUpsertDTO;
import com.quyen.shoplite.domain.response.ResCategoryDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_ShouldReturnCategory_WhenNameIsUnique() {
        // Arrange
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("  New Category  ");

        when(categoryRepository.existsByName("New Category")).thenReturn(false);

        Category savedCategory = Category.builder()
                .id(1)
                .name("New Category")
                .build();
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // Act
        ResCategoryDTO result = categoryService.create(req);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Category", result.getName());
        verify(categoryRepository).save(argThat(c -> c.getName().equals("New Category")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        // Arrange
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Existing Category");

        when(categoryRepository.existsByName("Existing Category")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            categoryService.create(req)
        );
        assertEquals("Category name already exists: Existing Category", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_ShouldReturnCategory_WhenIdExistsAndNameIsUnique() {
        // Arrange
        Integer id = 1;
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Updated Category");

        Category existingCategory = Category.builder().id(id).name("Old Name").build();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByNameAndIdNot("Updated Category", id)).thenReturn(false);

        Category savedCategory = Category.builder().id(id).name("Updated Category").build();
        when(categoryRepository.save(existingCategory)).thenReturn(savedCategory);

        // Act
        ResCategoryDTO result = categoryService.update(id, req);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Category", result.getName());
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Updated Category");

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            categoryService.update(id, req)
        );
        assertEquals("Category not found with id=99", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        // Arrange
        Integer id = 1;
        ReqCategoryUpsertDTO req = new ReqCategoryUpsertDTO();
        req.setName("Duplicate Name");

        Category existingCategory = Category.builder().id(id).name("Old Name").build();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByNameAndIdNot("Duplicate Name", id)).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            categoryService.update(id, req)
        );
        assertEquals("Category name already exists: Duplicate Name", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        // Arrange
        Integer id = 1;
        Category existingCategory = Category.builder().id(id).name("Name").build();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        // Act
        categoryService.delete(id);

        // Assert
        verify(categoryRepository).delete(existingCategory);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            categoryService.delete(id)
        );
        assertEquals("Category not found with id=99", exception.getMessage());
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}