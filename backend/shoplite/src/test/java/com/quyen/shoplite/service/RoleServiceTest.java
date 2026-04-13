package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.request.ReqRoleDTO;
import com.quyen.shoplite.domain.response.ResPermissionDTO;
import com.quyen.shoplite.domain.response.ResRoleDTO;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleService roleService;

    @Test
    void createRole_success() {
        // Arrange
        ReqRoleDTO req = new ReqRoleDTO();
        req.setName("ADMIN");
        req.setDescription("System Admin");
        req.setActive(true);

        Role savedRole = Role.builder()
                .id(1L)
                .name("ADMIN")
                .description("System Admin")
                .active(true)
                .build();

        when(roleRepository.existsByName("ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        // Act
        ResRoleDTO res = roleService.create(req);

        // Assert
        assertNotNull(res);
        assertEquals(1L, res.getId());
        assertEquals("ADMIN", res.getName());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void assignPermissionsToRole_success() {
        // Arrange
        ReqRoleDTO req = new ReqRoleDTO();
        req.setName("MANAGER");
        req.setPermissionIds(List.of(10L, 11L));

        Permission p1 = new Permission(); p1.setId(10L); p1.setName("Perm 1");
        Permission p2 = new Permission(); p2.setId(11L); p2.setName("Perm 2");
        List<Permission> permissions = List.of(p1, p2);

        Role savedRole = Role.builder()
                .id(2L)
                .name("MANAGER")
                .permissions(permissions)
                .build();

        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(permissionService.findAllByIds(List.of(10L, 11L))).thenReturn(permissions);
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        // Act
        ResRoleDTO res = roleService.create(req);

        // Assert
        assertNotNull(res);
        assertEquals(2, res.getPermissions().size());
        verify(permissionService, times(1)).findAllByIds(List.of(10L, 11L));
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void findById_roleNotFound_throwsException() {
        // Arrange
        Long roleId = 99L;
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            roleService.findById(roleId);
        });
        assertTrue(ex.getMessage().contains("Không tìm thấy Role"));
        verify(roleRepository, times(1)).findById(roleId);
    }
}
