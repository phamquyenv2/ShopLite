package com.quyen.shoplite.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.ImportOrder;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportOrderRepository extends JpaRepository<ImportOrder, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT io FROM ImportOrder io WHERE io.id = :id")
    Optional<ImportOrder> findByIdWithLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT io FROM ImportOrder io WHERE io.id = :id AND io.store.id = :storeId")
    Optional<ImportOrder> findByIdAndStoreIdWithLock(@Param("id") Integer id, @Param("storeId") Long storeId);

    List<ImportOrder> findBySupplier_Id(Integer supplierId);
    Optional<ImportOrder> findByIdAndStoreId(Integer id, Long storeId);
    List<ImportOrder> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<ImportOrder> findByStoreIdAndSupplier_Id(Long storeId, Integer supplierId);
}
