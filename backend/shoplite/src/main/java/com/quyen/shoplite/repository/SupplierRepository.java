package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    Optional<Supplier> findByIdAndStoreId(Integer id, Long storeId);
    List<Supplier> findAllByStoreIdOrderByIdAsc(Long storeId);
    boolean existsByStoreIdAndName(Long storeId, String name);
    boolean existsByStoreIdAndNameAndIdNot(Long storeId, String name, Integer id);
}
