package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqOfficeDTO;
import com.quyen.shoplite.domain.response.ResOfficeDTO;

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

    @Mock private OfficeRepository officeRepository;
    @Mock private CurrentStoreService currentStoreService;

    @InjectMocks
    private OfficeService officeService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void create_ShouldReturnOffice_WhenNameIsUnique() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(officeRepository.existsByStoreIdAndName(1L, "New Office")).thenReturn(false);

        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("  New Office  ");
        req.setOfficeLat(BigDecimal.valueOf(10.0));
        req.setOfficeLng(BigDecimal.valueOf(20.0));
        req.setRadius(500);

        Office savedOffice = Office.builder()
                .id(1).name("New Office")
                .officeLat(BigDecimal.valueOf(10.0))
                .officeLng(BigDecimal.valueOf(20.0))
                .radius(500).build();
        when(officeRepository.save(any(Office.class))).thenReturn(savedOffice);

        ResOfficeDTO result = officeService.create(req);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Office", result.getName());
        verify(officeRepository).save(argThat(o -> o.getName().equals("New Office")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(officeRepository.existsByStoreIdAndName(1L, "Existing Office")).thenReturn(true);

        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Existing Office");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            officeService.create(req));
        assertEquals("Office name already exists: Existing Office", exception.getMessage());
        verify(officeRepository, never()).save(any(Office.class));
    }

    @Test
    void update_ShouldReturnOffice_WhenIdExistsAndNameIsUnique() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Updated Office");

        Office existingOffice = Office.builder().id(id).name("Old Name").build();
        when(officeRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingOffice));
        when(officeRepository.existsByStoreIdAndNameAndIdNot(1L, "Updated Office", id)).thenReturn(false);

        Office savedOffice = Office.builder().id(id).name("Updated Office").build();
        when(officeRepository.save(existingOffice)).thenReturn(savedOffice);

        ResOfficeDTO result = officeService.update(id, req);

        assertNotNull(result);
        assertEquals("Updated Office", result.getName());
        verify(officeRepository).save(existingOffice);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Updated Office");

        when(officeRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            officeService.update(id, req));
        assertEquals("Office not found with id=99", exception.getMessage());
        verify(officeRepository, never()).save(any(Office.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqOfficeDTO req = new ReqOfficeDTO();
        req.setName("Duplicate Name");

        Office existingOffice = Office.builder().id(id).name("Old Name").build();
        when(officeRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingOffice));
        when(officeRepository.existsByStoreIdAndNameAndIdNot(1L, "Duplicate Name", id)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            officeService.update(id, req));
        assertEquals("Office name already exists: Duplicate Name", exception.getMessage());
        verify(officeRepository, never()).save(any(Office.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        Office existingOffice = Office.builder().id(id).name("Name").build();
        when(officeRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingOffice));

        officeService.delete(id);

        verify(officeRepository).delete(existingOffice);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        when(officeRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            officeService.delete(id));
        assertEquals("Office not found with id=99", exception.getMessage());
        verify(officeRepository, never()).delete(any(Office.class));
    }
}
