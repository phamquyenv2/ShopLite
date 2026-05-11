package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.ImportReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportReturnItemRepository extends JpaRepository<ImportReturnItem, Integer> {
    List<ImportReturnItem> findByImportReturnOrder_Id(Integer returnOrderId);

    @Query("SELECT ri FROM ImportReturnItem ri WHERE ri.importReturnOrder.id IN :orderIds")
    List<ImportReturnItem> findByImportReturnOrder_IdIn(@Param("orderIds") List<Integer> orderIds);
}
