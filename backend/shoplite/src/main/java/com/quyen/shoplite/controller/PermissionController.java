package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.PermissionService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqPermissionDTO;
import com.quyen.shoplite.domain.response.ResPermissionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @ApiMessage("Tạo permission thành công")
    public ResponseEntity<ResPermissionDTO> create(@Valid @RequestBody ReqPermissionDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin permission")
    public ResponseEntity<ResPermissionDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.findById(id));
    }

    @GetMapping("/all")
    @ApiMessage("Tất cả permissions")
    public ResponseEntity<java.util.List<ResPermissionDTO>> getAllNoPaging() {
        return ResponseEntity.ok(permissionService.findAll());
    }

    @GetMapping
    @ApiMessage("Danh sách permissions")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(permissionService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật permission thành công")
    public ResponseEntity<ResPermissionDTO> update(@PathVariable Long id,
                                                   @Valid @RequestBody ReqPermissionDTO req) {
        return ResponseEntity.ok(permissionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xoá permission thành công")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
