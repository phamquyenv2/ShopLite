package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.InventoryLogs;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogsRepository extends JpaRepository<InventoryLogs, Integer> {
    List<InventoryLogs> findAllByProduct_Id(Integer productId);
    List<InventoryLogs> findAllByType(TypeInventoryEnum type);
    List<InventoryLogs> findByAdjustment_Id(Integer adjustmentId);
    List<InventoryLogs> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<InventoryLogs> findAllByStoreIdAndProduct_Id(Long storeId, Integer productId);
    List<InventoryLogs> findByStoreIdAndAdjustment_Id(Long storeId, Integer adjustmentId);
}

