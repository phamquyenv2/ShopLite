package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.ImportOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportOrderRepository extends JpaRepository<ImportOrder, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT io FROM ImportOrder io WHERE io.id = :id")
    Optional<ImportOrder> findByIdWithLock(@Param("id") Integer id);

    List<ImportOrder> findBySupplier_Id(Integer supplierId);
}
