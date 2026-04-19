package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.util.constant.StatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.code = :code")
    Optional<Order> findByCodeWithLock(@Param("code") String code);

    Optional<Order> findByCode(String code);
    boolean existsByCode(String code);
    List<Order> findAllByStatus(StatusEnum status);
    List<Order> findByStatusIn(List<StatusEnum> statuses, org.springframework.data.domain.Sort sort);
    List<Order> findAllByUserId(Integer userId);
}
