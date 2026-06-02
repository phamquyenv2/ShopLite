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

    @Query("SELECT ii.product.id FROM ImportItem ii " +
           "WHERE ii.importOrder.store.id = :storeId " +
           "AND ii.importOrder.status NOT IN :completedStatuses " +
           "AND ii.importOrder.id != :excludeOrderId " +
           "AND ii.product.id IN :productIds")
    List<Integer> findProductIdsInUnfinishedOrders(
            @Param("storeId") Long storeId,
            @Param("productIds") List<Integer> productIds,
            @Param("completedStatuses") List<com.quyen.shoplite.util.constant.ImportOrderStatusEnum> completedStatuses,
            @Param("excludeOrderId") Integer excludeOrderId
    );
}
