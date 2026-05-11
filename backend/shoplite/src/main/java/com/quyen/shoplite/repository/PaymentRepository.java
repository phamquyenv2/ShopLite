package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.util.constant.PaymentStatusEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT p FROM Payment p WHERE p.referenceType = :refType AND p.referenceId IN :refIds AND p.store.id = :storeId")
    List<Payment> findByStoreIdAndReferenceTypeAndReferenceIdIn(
            @Param("storeId") Long storeId,
            @Param("refType") RefTypeEnum refType,
            @Param("refIds") List<Integer> refIds);

    /**
     * Tìm Payment theo reference (polymorphic reference thay thế cho findByOrder_Id).
     */
    Optional<Payment> findByReferenceTypeAndReferenceId(RefTypeEnum referenceType, Integer referenceId);

    List<Payment> findAllByReferenceTypeAndReferenceId(RefTypeEnum referenceType, Integer referenceId);

    boolean existsByReferenceTypeAndReferenceIdAndStatusIn(
            RefTypeEnum referenceType, Integer referenceId, List<PaymentStatusEnum> statuses);

    Optional<Payment> findByStoreIdAndReferenceTypeAndReferenceId(
            Long storeId, RefTypeEnum referenceType, Integer referenceId);

    List<Payment> findAllByStoreIdAndReferenceTypeAndReferenceId(
            Long storeId, RefTypeEnum referenceType, Integer referenceId);

    boolean existsByStoreIdAndReferenceTypeAndReferenceIdAndStatusIn(
            Long storeId, RefTypeEnum referenceType, Integer referenceId, List<PaymentStatusEnum> statuses);
}
