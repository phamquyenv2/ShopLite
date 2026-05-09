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
import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.store.id = :storeId")
    Optional<Order> findByIdAndStoreIdWithLock(@Param("id") Integer id, @Param("storeId") Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.code = :code")
    Optional<Order> findByCodeWithLock(@Param("code") String code);

    Optional<Order> findByCode(String code);
    Optional<Order> findByRequestId(String requestId);
    boolean existsByCode(String code);
    List<Order> findAllByStatus(StatusEnum status);
    List<Order> findByStatusIn(List<StatusEnum> statuses, org.springframework.data.domain.Sort sort);
    List<Order> findAllByUserId(Integer userId);
    Optional<Order> findByIdAndStoreId(Integer id, Long storeId);
    Optional<Order> findByStoreIdAndCode(Long storeId, String code);
    Optional<Order> findByStoreIdAndRequestId(Long storeId, String requestId);
    boolean existsByStoreIdAndCode(Long storeId, String code);
    List<Order> findAllByStoreId(Long storeId, org.springframework.data.domain.Sort sort);
    List<Order> findByStoreIdAndStatusIn(Long storeId, List<StatusEnum> statuses, org.springframework.data.domain.Sort sort);
    List<Order> findAllByStoreIdAndCreatedAtBetween(
            Long storeId,
            LocalDateTime from,
            LocalDateTime to,
            org.springframework.data.domain.Sort sort);
    List<Order> findByStoreIdAndStatusInAndCreatedAtBetween(
            Long storeId,
            List<StatusEnum> statuses,
            LocalDateTime from,
            LocalDateTime to,
            org.springframework.data.domain.Sort sort);
}
