package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.request.ReqRoleDTO;
import com.quyen.shoplite.domain.response.ResRoleDTO;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    @Transactional
    public ResRoleDTO create(ReqRoleDTO req) {
        if (roleRepository.existsByName(req.getName())) {
            throw new BadRequestException("Role '" + req.getName() + "' Ä‘Ã£ tá»“n táº¡i");
        }
        List<Permission> permissions = resolvePermissions(req.getPermissionIds());
        Role role = Role.builder()
                .name(req.getName())
                .description(req.getDescription())
                .active(req.isActive())
                .permissions(permissions)
                .createdAt(LocalDateTime.now())
                .build();
        return DTOMapper.toResRoleDTO(roleRepository.save(role));
    }

    public ResRoleDTO findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Role id=" + id));
        return DTOMapper.toResRoleDTO(role);
    }

    @Transactional
    public ResRoleDTO update(Long id, ReqRoleDTO req) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Role id=" + id));

        if (req.getVersion() != null && !req.getVersion().equals(role.getVersion())) {
            throw new BadRequestException("Role has been modified by another user. Please refresh and try again.");
        }

        if (req.getName() != null && !req.getName().isBlank()
                && roleRepository.existsByNameAndIdNot(req.getName(), id)) {
            throw new BadRequestException("Role '" + req.getName() + "' Ä‘Ã£ tá»“n táº¡i");
        }

        if (req.getName() != null && !req.getName().isBlank()) {
            role.setName(req.getName());
        }
        if (req.getDescription() != null) {
            role.setDescription(req.getDescription());
        }
        role.setActive(req.isActive());
        role.setUpdatedAt(LocalDateTime.now());

        if (req.getPermissionIds() != null) {
            role.setPermissions(resolvePermissions(req.getPermissionIds()));
        }

        return DTOMapper.toResRoleDTO(roleRepository.save(role));
    }

    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Role id=" + id);
        }
        roleRepository.deleteById(id);
    }

    public Map<String, Object> getAll(Pageable pageable) {
        Page<Role> page = roleRepository.findAll(pageable);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("page", pageable.getPageNumber());
        result.put("size", pageable.getPageSize());
        result.put("data", page.getContent().stream().map(DTOMapper::toResRoleDTO).toList());
        return result;
    }

    public Role findEntityByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }

    private List<Permission> resolvePermissions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return permissionService.findAllByIds(ids);
    }
}
