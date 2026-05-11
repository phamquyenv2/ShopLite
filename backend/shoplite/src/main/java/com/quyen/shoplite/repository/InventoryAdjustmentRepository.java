package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.InventoryAdjustment;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Integer> {
    Optional<InventoryAdjustment> findByIdAndStoreId(Integer id, Long storeId);
    List<InventoryAdjustment> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
}
