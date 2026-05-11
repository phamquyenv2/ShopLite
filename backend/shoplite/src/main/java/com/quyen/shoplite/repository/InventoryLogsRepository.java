package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.InventoryLogs;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryLogsRepository extends JpaRepository<InventoryLogs, Integer> {
    List<InventoryLogs> findAllByProduct_Id(Integer productId);
    List<InventoryLogs> findAllByType(TypeInventoryEnum type);
    List<InventoryLogs> findByAdjustment_Id(Integer adjustmentId);
    List<InventoryLogs> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<InventoryLogs> findAllByStoreIdAndProduct_Id(Long storeId, Integer productId);
    List<InventoryLogs> findByStoreIdAndAdjustment_Id(Long storeId, Integer adjustmentId);

    @Query("SELECT il FROM InventoryLogs il WHERE il.store.id = :storeId AND il.adjustment.id IN :adjustmentIds")
    List<InventoryLogs> findByStoreIdAndAdjustment_IdIn(@Param("storeId") Long storeId, @Param("adjustmentIds") List<Integer> adjustmentIds);
}

