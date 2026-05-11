package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.EmployeeSalaryHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSalaryHistoryRepository extends JpaRepository<EmployeeSalaryHistory, Integer> {

    @EntityGraph(attributePaths = {"employee", "employee.storeMember", "employee.storeMember.user"})
    List<EmployeeSalaryHistory> findByStore_IdAndEmployee_IdOrderByEffectiveFromDescIdDesc(Long storeId, Integer employeeId);

    @EntityGraph(attributePaths = {"employee", "employee.storeMember", "employee.storeMember.user"})
    Optional<EmployeeSalaryHistory> findFirstByStore_IdAndEmployee_IdAndEffectiveToIsNullOrderByEffectiveFromDescIdDesc(
            Long storeId, Integer employeeId);

    @EntityGraph(attributePaths = {"employee", "employee.storeMember", "employee.storeMember.user"})
    @Query("""
            SELECT h FROM EmployeeSalaryHistory h
            WHERE h.store.id = :storeId
              AND h.employee.id = :employeeId
              AND h.effectiveFrom <= :date
              AND (h.effectiveTo IS NULL OR h.effectiveTo >= :date)
            ORDER BY h.effectiveFrom DESC, h.id DESC
            """)
    List<EmployeeSalaryHistory> findEffectiveAt(
            @Param("storeId") Long storeId,
            @Param("employeeId") Integer employeeId,
            @Param("date") LocalDate date);
}
