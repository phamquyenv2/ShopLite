package com.quyen.shoplite.repository;

import com.quyen.shoplite.util.constant.TypeTransactionEnum;

import com.quyen.shoplite.domain.Transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findAllByFundAccount_Id(Integer fundAccountId);

    List<Transaction> findAllByPayment_Id(Integer paymentId);

    List<Transaction> findAllByType(TypeTransactionEnum type);

    boolean existsByPayment_Id(Integer paymentId);

    Optional<Transaction> findByTransactionCode(String transactionCode);

    boolean existsByPayment_IdAndType(Integer paymentId, TypeTransactionEnum type);

    Optional<Transaction> findByIdAndStoreId(Integer id, Long storeId);

    List<Transaction> findAllByStoreIdOrderByTransactionTimeDesc(Long storeId);

    List<Transaction> findAllByStoreIdAndFundAccount_Id(Long storeId, Integer fundAccountId);

    List<Transaction> findAllByStoreIdAndPayment_Id(Long storeId, Integer paymentId);

    List<Transaction> findAllByStoreIdAndType(Long storeId, TypeTransactionEnum type);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionCode LIKE :prefix%")
    long countByTransactionCodeStartingWith(@org.springframework.data.repository.query.Param("prefix") String prefix);
}
