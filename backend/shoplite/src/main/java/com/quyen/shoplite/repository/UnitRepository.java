package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Integer> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    Optional<Unit> findByIdAndStoreId(Integer id, Long storeId);
    List<Unit> findAllByStoreIdOrderByNameAsc(Long storeId);
    boolean existsByStoreIdAndName(Long storeId, String name);
    boolean existsByStoreIdAndNameAndIdNot(Long storeId, String name, Integer id);
}
