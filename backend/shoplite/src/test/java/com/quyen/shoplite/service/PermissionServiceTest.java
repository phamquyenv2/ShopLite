package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.PermissionRepository;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.request.ReqPermissionDTO;
import com.quyen.shoplite.domain.response.ResPermissionDTO;

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
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void createPermission_success() {
        // Arrange
        ReqPermissionDTO req = new ReqPermissionDTO();
        req.setName("View Users");
        req.setApiPath("/api/v1/users");
        req.setMethod("GET");
        req.setModule("USER");

        Permission savedPermission = Permission.builder()
                .id(1L)
                .name("View Users")
                .apiPath("/api/v1/users")
                .method("GET")
                .module("USER")
                .build();

        when(permissionRepository.existsByModuleAndApiPathAndMethod("USER", "/api/v1/users", "GET")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(savedPermission);

        // Act
        ResPermissionDTO res = permissionService.create(req);

        // Assert
        assertNotNull(res);
        assertEquals(1L, res.getId());
        assertEquals("View Users", res.getName());
        verify(permissionRepository, times(1)).save(any(Permission.class));
    }

    @Test
    void findById_permissionNotFound_throwsException() {
        // Arrange
        Long permissionId = 99L;
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            permissionService.findById(permissionId);
        });
        assertTrue(ex.getMessage().contains("Không tìm thấy Permission"));
        verify(permissionRepository, times(1)).findById(permissionId);
    }
}
