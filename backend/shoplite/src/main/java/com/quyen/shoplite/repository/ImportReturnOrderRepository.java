package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quyen.shoplite.domain.ImportReturnOrder;

import java.util.List;
import java.util.Optional;

public interface ImportReturnOrderRepository extends JpaRepository<ImportReturnOrder, Integer> {
    List<ImportReturnOrder> findBySupplier_Id(Integer supplierId);
    List<ImportReturnOrder> findByImportOrder_Id(Integer importOrderId);
    Optional<ImportReturnOrder> findByIdAndStoreId(Integer id, Long storeId);
    List<ImportReturnOrder> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<ImportReturnOrder> findByStoreIdAndSupplier_Id(Long storeId, Integer supplierId);
    List<ImportReturnOrder> findByStoreIdAndImportOrder_Id(Long storeId, Integer importOrderId);
}
