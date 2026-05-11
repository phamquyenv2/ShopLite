package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.PermissionRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.IdInvalidException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.request.ReqPermissionDTO;
import com.quyen.shoplite.domain.response.ResPermissionDTO;

import com.quyen.shoplite.util.DTOMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    // ─── Create ────────────────────────────────────────────────────────────────
    @CacheEvict(value = "permissions", allEntries = true)
    public ResPermissionDTO create(ReqPermissionDTO req) {
        if (permissionRepository.existsByModuleAndApiPathAndMethod(
                req.getModule(), req.getApiPath(), req.getMethod())) {
            throw new BadRequestException(
                    "Permission [" + req.getMethod() + " " + req.getApiPath() + "] đã tồn tại trong module " + req.getModule());
        }
        Permission p = Permission.builder()
                .name(req.getName())
                .apiPath(req.getApiPath())
                .method(req.getMethod().toUpperCase())
                .module(req.getModule().toUpperCase())
                .createdAt(LocalDateTime.now())
                .build();
        return DTOMapper.toResPermissionDTO(permissionRepository.save(p));
    }

    // ─── Get by ID ─────────────────────────────────────────────────────────────
    public ResPermissionDTO findById(Long id) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Permission id=" + id));
        return DTOMapper.toResPermissionDTO(p);
    }

    // ─── Update ────────────────────────────────────────────────────────────────
    @CacheEvict(value = "permissions", allEntries = true)
    public ResPermissionDTO update(Long id, ReqPermissionDTO req) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Permission id=" + id));
        p.setName(req.getName());
        p.setApiPath(req.getApiPath());
        p.setMethod(req.getMethod().toUpperCase());
        p.setModule(req.getModule().toUpperCase());
        p.setUpdatedAt(LocalDateTime.now());
        return DTOMapper.toResPermissionDTO(permissionRepository.save(p));
    }

    // ─── Delete ────────────────────────────────────────────────────────────────
    @CacheEvict(value = "permissions", allEntries = true)
    public void delete(Long id) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Permission id=" + id));
        // Xóa relationship với roles trước
        p.getRoles().forEach(role -> role.getPermissions().remove(p));
        permissionRepository.delete(p);
    }

    // ─── Get All (paginated) ───────────────────────────────────────────────────
    public Map<String, Object> getAll(Pageable pageable) {
        Page<Permission> page = permissionRepository.findAll(pageable);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("page", pageable.getPageNumber());
        result.put("size", pageable.getPageSize());
        result.put("data", page.getContent().stream().map(DTOMapper::toResPermissionDTO).toList());
        return result;
    }

    // ─── Get All (no pagination) ────────────────────────────────────────────────
    @Cacheable(value = "permissions")
    public List<ResPermissionDTO> findAll() {
        return permissionRepository.findAll().stream()
                .map(DTOMapper::toResPermissionDTO)
                .toList();
    }

    // ─── Internal: find entities by IDs (used by RoleService) ─────────────────
    public List<Permission> findAllByIds(List<Long> ids) {
        return permissionRepository.findAllById(ids);
    }

}
