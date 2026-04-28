package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.ImportReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportReturnItemRepository extends JpaRepository<ImportReturnItem, Integer> {
    List<ImportReturnItem> findByImportReturnOrder_Id(Integer returnOrderId);
}
