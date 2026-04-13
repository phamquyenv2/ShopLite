package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.request.ReqProductUpsertDTO;
import com.quyen.shoplite.domain.response.ResProductDTO;
import com.quyen.shoplite.domain.response.ResProductPageDTO;
import com.quyen.shoplite.service.ProductService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @PostMapping
    @ApiMessage("Create product successfully")
    public ResponseEntity<ResProductDTO> create(@Valid @RequestBody ReqProductUpsertDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
    }

    /**
     * GET /api/v1/products/{id}
     * Láº¥y thÃ´ng tin má»™t sáº£n pháº©m theo id.
     */
    @GetMapping("/{id}")
    @ApiMessage("Get product successfully")
    public ResponseEntity<ResProductDTO> findById( @Positive(message = " must be greater than 0") Integer id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /**
     * GET /api/v1/products
     *
     * @param keyword    Search by name or SKU
     * @param categoryId Filter by category
     * @param minPrice   Minimum price
     * @param maxPrice   Maximum price
     * @param page       Page (starting from 0)
     * @param size       Number of items per page (default: 10)
     * @param sortBy     Sort by (default: createdAt)
     * @param sortDir    Sort direction: asc | desc (default: desc)
     */
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

    /**
     * PUT /api/v1/products/{id}
     * Update product (name, price, category).
     * Stock cannot be changed.
     */
    @PutMapping("/{id}")
    @ApiMessage("Update product successfully")
    public ResponseEntity<ResProductDTO> update( @Positive(message = " must be greater than 0") Integer id,
                                                @Valid @RequestBody ReqProductUpsertDTO req) {
        return ResponseEntity.ok(productService.update(id, req));
    }

    /**
     * DELETE /api/v1/products/{id}
     * Soft delete product (is_deleted = true).
     */
    @DeleteMapping("/{id}")
    @ApiMessage("Soft delete product successfully")
    public ResponseEntity<Void> softDelete( @Positive(message = " must be greater than 0") Integer id) {
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}



