package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.SupplierRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.Supplier;
import com.quyen.shoplite.domain.request.ReqSupplierDTO;
import com.quyen.shoplite.domain.response.ResSupplierDTO;

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
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CurrentStoreService currentStoreService;

    @InjectMocks
    private SupplierService supplierService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void create_ShouldReturnSupplier_WhenNameIsUnique() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(supplierRepository.existsByStoreIdAndName(1L, "New Supplier")).thenReturn(false);

        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("  New Supplier  ");
        req.setPhone("0987654321");
        req.setEmail("test@example.com");

        Supplier savedSupplier = Supplier.builder()
                .id(1)
                .name("New Supplier")
                .phone("0987654321")
                .email("test@example.com")
                .build();
        when(supplierRepository.save(any(Supplier.class))).thenReturn(savedSupplier);

        ResSupplierDTO result = supplierService.create(req);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("New Supplier", result.getName());
        assertEquals("0987654321", result.getPhone());
        assertEquals("test@example.com", result.getEmail());
        verify(supplierRepository).save(argThat(s -> s.getName().equals("New Supplier")));
    }

    @Test
    void create_ShouldThrowBadRequest_WhenNameExists() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(supplierRepository.existsByStoreIdAndName(1L, "Existing Supplier")).thenReturn(true);

        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Existing Supplier");

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            supplierService.create(req)
        );
        assertEquals("Supplier name already exists: Existing Supplier", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void update_ShouldReturnSupplier_WhenIdExistsAndNameIsUnique() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Updated Supplier");

        Supplier existingSupplier = Supplier.builder().id(id).name("Old Name").build();
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByStoreIdAndNameAndIdNot(1L, "Updated Supplier", id)).thenReturn(false);

        Supplier savedSupplier = Supplier.builder().id(id).name("Updated Supplier").build();
        when(supplierRepository.save(existingSupplier)).thenReturn(savedSupplier);

        ResSupplierDTO result = supplierService.update(id, req);

        assertNotNull(result);
        assertEquals("Updated Supplier", result.getName());
        verify(supplierRepository).save(existingSupplier);
    }

    @Test
    void update_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Updated Supplier");

        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            supplierService.update(id, req)
        );
        assertEquals("Supplier not found with id=99", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void update_ShouldThrowBadRequest_WhenNameExistsForAnotherId() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        ReqSupplierDTO req = new ReqSupplierDTO();
        req.setName("Duplicate Name");

        Supplier existingSupplier = Supplier.builder().id(id).name("Old Name").build();
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingSupplier));
        when(supplierRepository.existsByStoreIdAndNameAndIdNot(1L, "Duplicate Name", id)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            supplierService.update(id, req)
        );
        assertEquals("Supplier name already exists: Duplicate Name", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void delete_ShouldCallRepositoryDelete_WhenIdExists() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 1;
        Supplier existingSupplier = Supplier.builder().id(id).name("Name").build();
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.of(existingSupplier));

        supplierService.delete(id);

        verify(supplierRepository).delete(existingSupplier);
    }

    @Test
    void delete_ShouldThrowResourceNotFound_WhenIdDoesNotExist() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Integer id = 99;
        when(supplierRepository.findByIdAndStoreId(id, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            supplierService.delete(id)
        );
        assertEquals("Supplier not found with id=99", exception.getMessage());
        verify(supplierRepository, never()).delete(any(Supplier.class));
    }
}
