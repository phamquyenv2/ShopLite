package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.Payroll;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Integer> {
    List<Payroll> findByEmployee_Id(Integer employeeId);
    List<Payroll> findByEmployee_StoreMember_Store_IdAndEmployee_Id(Long storeId, Integer employeeId);
    Optional<Payroll> findByEmployee_IdAndPeriod(Integer employeeId, LocalDate period);
    Optional<Payroll> findByEmployee_StoreMember_Store_IdAndEmployee_IdAndPeriod(Long storeId, Integer employeeId, LocalDate period);
    List<Payroll> findAllByOrderByPeriodDescEmployee_IdAsc();

    @EntityGraph(attributePaths = {"employee", "employee.storeMember", "employee.storeMember.user"})
    List<Payroll> findAllByEmployee_StoreMember_Store_IdOrderByPeriodDescEmployee_IdAsc(Long storeId);
}
