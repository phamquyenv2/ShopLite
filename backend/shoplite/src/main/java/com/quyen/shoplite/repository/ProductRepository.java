package com.quyen.shoplite.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quyen.shoplite.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>,
        JpaSpecificationExecutor<Product> {

    /**
     * Acquires a PESSIMISTIC_WRITE (SELECT … FOR UPDATE) lock on the product row.
     * Use this whenever stock will be read-then-written inside a @Transactional method
     * to prevent concurrent oversell / stock corruption.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.store.id = :storeId")
    Optional<Product> findByIdAndStoreIdWithLock(@Param("id") Integer id, @Param("storeId") Long storeId);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Integer id);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Integer id);

    List<Product> findAllByIsDeletedFalse();

    List<Product> findAllByCategoryId(Integer categoryId);

    Optional<Product> findByBarcodeAndIsDeletedFalse(String barcode);

    Optional<Product> findByIdAndStoreIdAndIsDeletedFalse(Integer id, Long storeId);
    boolean existsByStoreIdAndSku(Long storeId, String sku);
    boolean existsByStoreIdAndSkuAndIdNot(Long storeId, String sku, Integer id);
    boolean existsByStoreIdAndBarcode(Long storeId, String barcode);
    boolean existsByStoreIdAndBarcodeAndIdNot(Long storeId, String barcode, Integer id);
    @EntityGraph(attributePaths = {"category", "unit"})
    List<Product> findAllByStoreIdAndIsDeletedFalse(Long storeId);
    List<Product> findAllByStoreIdAndCategoryId(Long storeId, Integer categoryId);
    Optional<Product> findByStoreIdAndBarcodeAndIsDeletedFalse(Long storeId, String barcode);
}
