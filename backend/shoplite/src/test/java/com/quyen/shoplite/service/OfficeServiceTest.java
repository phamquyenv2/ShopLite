package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.request.ReqOfficeDTO;
import com.quyen.shoplite.domain.response.ResOfficeDTO;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

    @Mock
    private OfficeRepository officeRepository;

    @InjectMocks
    private OfficeService officeService;

    @Test
    void create_ShouldReturnOffice_WhenNameIsUnique() {
        // Arrange
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("  New Office  ");
        req.setOfficeLat(BigDecimal.valueOf(10.0));
        req.setOfficeLng(BigDecimal.valueOf(20.0));
        req.setRadius(500);

        when(officeRepository.existsByName("New Office")).thenReturn(false);

        Office savedOffice = Office.builder()
                .id(1)
                .name("New Office")
                .officeLat(BigDecimal.valueOf(10.0))
                .officeLng(BigDecimal.valueOf(20.0))
                .radius(500)
                .build();
        when(officeRepository.save(any(Office.class))).thenReturn(savedOffice);

        // Act
        ResOfficeDTO result = officeService.create(req);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Office", result.getName());
        assertEquals(BigDecimal.valueOf(10.0), result.getOfficeLat());
        verify(officeRepository).save(argThat(o -> o.getName().equals("New Office")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        // Arrange
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Existing Office");

        when(officeRepository.existsByName("Existing Office")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            officeService.create(req)
        );
        assertEquals("Office name already exists: Existing Office", exception.getMessage());
        verify(officeRepository, never()).save(any(Office.class));
    }

    @Test
    void update_ShouldReturnOffice_WhenIdExistsAndNameIsUnique() {
        // Arrange
        Integer id = 1;
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Updated Office");

        Office existingOffice = Office.builder().id(id).name("Old Name").build();
        when(officeRepository.findById(id)).thenReturn(Optional.of(existingOffice));
        when(officeRepository.existsByNameAndIdNot("Updated Office", id)).thenReturn(false);

        Office savedOffice = Office.builder().id(id).name("Updated Office").build();
        when(officeRepository.save(existingOffice)).thenReturn(savedOffice);

        // Act
        ResOfficeDTO result = officeService.update(id, req);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Office", result.getName());
        verify(officeRepository).save(existingOffice);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Updated Office");

        when(officeRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            officeService.update(id, req)
        );
        assertEquals("Office not found with id=99", exception.getMessage());
        verify(officeRepository, never()).save(any(Office.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        // Arrange
        Integer id = 1;
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Duplicate Name");

        Office existingOffice = Office.builder().id(id).name("Old Name").build();
        when(officeRepository.findById(id)).thenReturn(Optional.of(existingOffice));
        when(officeRepository.existsByNameAndIdNot("Duplicate Name", id)).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            officeService.update(id, req)
        );
        assertEquals("Office name already exists: Duplicate Name", exception.getMessage());
        verify(officeRepository, never()).save(any(Office.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        // Arrange
        Integer id = 1;
        Office existingOffice = Office.builder().id(id).name("Name").build();
        when(officeRepository.findById(id)).thenReturn(Optional.of(existingOffice));

        // Act
        officeService.delete(id);

        // Assert
        verify(officeRepository).delete(existingOffice);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        // Arrange
        Integer id = 99;
        when(officeRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            officeService.delete(id)
        );
        assertEquals("Office not found with id=99", exception.getMessage());
        verify(officeRepository, never()).delete(any(Office.class));
    }
}
