package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Transaction;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findAllByOrder_Id(Integer orderId);
    List<Transaction> findAllByType(TypeTransactionEnum type);
    boolean existsByImportOrder_Id(Integer importOrderId);
}

