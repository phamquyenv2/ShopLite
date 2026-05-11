package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.ImportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportItemRepository extends JpaRepository<ImportItem, Integer> {
    List<ImportItem> findByImportOrder_Id(Integer importOrderId);

    @Query("SELECT ii FROM ImportItem ii WHERE ii.importOrder.id IN :orderIds")
    List<ImportItem> findByImportOrder_IdIn(@Param("orderIds") List<Integer> orderIds);
}
