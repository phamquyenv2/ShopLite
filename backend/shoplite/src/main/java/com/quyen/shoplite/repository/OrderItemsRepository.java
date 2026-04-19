package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Integer> {
    List<OrderItems> findAllByOrderId(Integer orderId);
    void deleteAllByOrder_Id(Integer orderId);
}
