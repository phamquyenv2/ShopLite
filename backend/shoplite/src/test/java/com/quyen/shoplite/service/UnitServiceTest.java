package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Unit;
import com.quyen.shoplite.domain.request.ReqUnitUpsertDTO;
import com.quyen.shoplite.domain.response.ResUnitDTO;
import com.quyen.shoplite.repository.UnitRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private UnitService unitService;

    @Test
    void create_ShouldReturnUnit_WhenNameIsUnique() {
        // Arrange
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("  New Unit  ");
        req.setDescription("Unit Description");

        when(unitRepository.existsByName("New Unit")).thenReturn(false);

        Unit savedUnit = Unit.builder()
                .id(1)
                .name("New Unit")
                .description("Unit Description")
                .build();
        when(unitRepository.save(any(Unit.class))).thenReturn(savedUnit);

        // Act
        ResUnitDTO result = unitService.create(req);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Unit", result.getName());
        verify(unitRepository).save(argThat(u -> u.getName().equals("New Unit")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        // Arrange
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Existing Unit");

        when(unitRepository.existsByName("Existing Unit")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            unitService.create(req)
        );
        assertEquals("Unit name already exists: Existing Unit", exception.getMessage());
        verify(unitRepository, never()).save(any(Unit.class));
    }

    @Test
    void update_ShouldReturnUnit_WhenIdExistsAndNameIsUnique() {
        // Arrange
        Integer id = 1;
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Updated Unit");

        Unit existingUnit = Unit.builder().id(id).name("Old Name").build();
        when(unitRepository.findById(id)).thenReturn(Optional.of(existingUnit));
        when(unitRepository.existsByNameAndIdNot("Updated Unit", id)).thenReturn(false);

        Unit savedUnit = Unit.builder().id(id).name("Updated Unit").build();
        when(unitRepository.save(existingUnit)).thenReturn(savedUnit);

        // Act
        ResUnitDTO result = unitService.update(id, req);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Unit", result.getName());
        verify(unitRepository).save(existingUnit);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Updated Unit");

        when(unitRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            unitService.update(id, req)
        );
        assertEquals("Unit not found with id=99", exception.getMessage());
        verify(unitRepository, never()).save(any(Unit.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        // Arrange
        Integer id = 1;
        ReqUnitUpsertDTO req = new ReqUnitUpsertDTO();
        req.setName("Duplicate Name");

        Unit existingUnit = Unit.builder().id(id).name("Old Name").build();
        when(unitRepository.findById(id)).thenReturn(Optional.of(existingUnit));
        when(unitRepository.existsByNameAndIdNot("Duplicate Name", id)).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            unitService.update(id, req)
        );
        assertEquals("Unit name already exists: Duplicate Name", exception.getMessage());
        verify(unitRepository, never()).save(any(Unit.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        // Arrange
        Integer id = 1;
        Unit existingUnit = Unit.builder().id(id).name("Name").build();
        when(unitRepository.findById(id)).thenReturn(Optional.of(existingUnit));

        // Act
        unitService.delete(id);

        // Assert
        verify(unitRepository).delete(existingUnit);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        when(unitRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            unitService.delete(id)
        );
        assertEquals("Unit not found with id=99", exception.getMessage());
        verify(unitRepository, never()).delete(any(Unit.class));
    }
}
