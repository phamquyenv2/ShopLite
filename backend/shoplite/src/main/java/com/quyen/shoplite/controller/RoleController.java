package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.RoleService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqRoleDTO;
import com.quyen.shoplite.domain.response.ResRoleDTO;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @ApiMessage("Tạo role thành công")
    public ResponseEntity<ResRoleDTO> create(@Valid @RequestBody ReqRoleDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin role")
    public ResponseEntity<ResRoleDTO> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @GetMapping
    @ApiMessage("Danh sách roles")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(roleService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật role thành công")
    public ResponseEntity<ResRoleDTO> update(@PathVariable("id") Long id,
                                             @Valid @RequestBody ReqRoleDTO req) {
        return ResponseEntity.ok(roleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xoá role thành công")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
