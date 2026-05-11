package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Integer> {
    List<OrderItems> findAllByOrderId(Integer orderId);
    void deleteAllByOrder_Id(Integer orderId);

    @Query("SELECT oi FROM OrderItems oi WHERE oi.order.id IN :orderIds")
    List<OrderItems> findAllByOrderIdIn(@Param("orderIds") List<Integer> orderIds);
}
