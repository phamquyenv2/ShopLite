package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.CategoryService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqCategoryUpsertDTO;
import com.quyen.shoplite.domain.response.ResCategoryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ApiMessage("Create category success")
    public ResponseEntity<ResCategoryDTO> create(@Valid @RequestBody ReqCategoryUpsertDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get category success")
    public ResponseEntity<ResCategoryDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get categories success")
    public ResponseEntity<List<ResCategoryDTO>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @PutMapping("/{id}")
    @ApiMessage("Update category success")
    public ResponseEntity<ResCategoryDTO> update(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id, @Valid @RequestBody ReqCategoryUpsertDTO req) {
        return ResponseEntity.ok(categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete category success")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


