package com.quyen.shoplite.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import com.quyen.shoplite.domain.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> filter(Long storeId, String keyword, Integer categoryId,
            Double minPrice, Double maxPrice, Integer unitId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn lọc theo store và loại bỏ sản phẩm đã xóa mềm
            // Dùng cb.isFalse() để tránh vấn đề type-mismatch với primitive boolean field
            predicates.add(cb.equal(root.get("store").get("id"), storeId));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // Lọc theo keyword (tên hoặc SKU)
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }

            // Lọc theo category
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Lọc theo giá tối thiểu
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }

            // Lọc theo giá tối đa
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }

            // Lọc theo unitId
            if (unitId != null) {
                predicates.add(cb.equal(root.get("unit").get("id"), unitId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
