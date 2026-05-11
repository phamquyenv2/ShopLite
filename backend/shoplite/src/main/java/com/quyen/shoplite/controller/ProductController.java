package com.quyen.shoplite.controller;

import com.quyen.shoplite.service.ProductService;
import com.quyen.shoplite.util.annotation.ApiMessage;

import com.quyen.shoplite.domain.request.ReqProductUpsertDTO;
import com.quyen.shoplite.domain.response.ResProductDTO;
import com.quyen.shoplite.domain.response.ResProductPageDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/v1/products/search?keyword=&barcode=
     * POS-optimized: barcode exact match or keyword fuzzy (name/SKU). Max 20 results.
     */
    @GetMapping("/search")
    @ApiMessage("Search products for POS success")
    public ResponseEntity<List<ResProductDTO>> searchForPOS(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "barcode", required = false) String barcode) {
        return ResponseEntity.ok(productService.searchForPOS(keyword, barcode));
    }

    @PostMapping
    @ApiMessage("Create product successfully")
    public ResponseEntity<ResProductDTO> create(@Valid @RequestBody ReqProductUpsertDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get product successfully")
    public ResponseEntity<ResProductDTO> findById(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping
    @ApiMessage("Get products successfully")
    public ResponseEntity<ResProductPageDTO> getProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(value = "unitId", required = false) Integer unitId) {

        return ResponseEntity.ok(
                productService.getProducts(keyword, categoryId, minPrice, maxPrice, page, size, sortBy, sortDir, unitId)
        );
    }

    @PutMapping("/{id}")
    @ApiMessage("Update product successfully")
    public ResponseEntity<ResProductDTO> update(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id,
            @Valid @RequestBody ReqProductUpsertDTO req) {
        return ResponseEntity.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Soft delete product successfully")
    public ResponseEntity<Void> softDelete(@PathVariable("id") @Positive(message = " must be greater than 0") Integer id) {
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
