package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    Optional<Category> findByIdAndStoreId(Integer id, Long storeId);
    List<Category> findAllByStoreIdOrderByNameAsc(Long storeId);
    boolean existsByStoreIdAndName(Long storeId, String name);
    boolean existsByStoreIdAndNameAndIdNot(Long storeId, String name, Integer id);
}
