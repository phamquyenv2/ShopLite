package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.FundAccount;
import com.quyen.shoplite.util.constant.FundTypeEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundAccountRepository extends JpaRepository<FundAccount, Integer> {

    List<FundAccount> findAllByIsActiveTrue();

    List<FundAccount> findAllByType(FundTypeEnum type);

    Optional<FundAccount> findFirstByTypeAndIsActiveTrue(FundTypeEnum type);

    List<FundAccount> findAllByStoreId(Long storeId);

    List<FundAccount> findAllByStoreIdAndIsActiveTrue(Long storeId);

    List<FundAccount> findAllByStoreIdAndType(Long storeId, FundTypeEnum type);

    Optional<FundAccount> findFirstByStoreIdAndTypeAndIsActiveTrue(Long storeId, FundTypeEnum type);

    Optional<FundAccount> findByIdAndStoreId(Integer id, Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FundAccount f WHERE f.id = :id")
    Optional<FundAccount> findByIdWithLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FundAccount f WHERE f.id = :id AND f.store.id = :storeId")
    Optional<FundAccount> findByIdAndStoreIdWithLock(@Param("id") Integer id, @Param("storeId") Long storeId);
}
