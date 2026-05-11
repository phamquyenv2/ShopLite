package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.Office;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Integer> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    Optional<Office> findByIdAndStoreId(Integer id, Long storeId);
    List<Office> findAllByStoreIdOrderByIdAsc(Long storeId);
    boolean existsByStoreIdAndName(Long storeId, String name);
    boolean existsByStoreIdAndNameAndIdNot(Long storeId, String name, Integer id);
}
