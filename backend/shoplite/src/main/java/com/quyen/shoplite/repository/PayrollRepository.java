package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Integer> {
    List<Payroll> findByEmployee_Id(Integer employeeId);
    Optional<Payroll> findByEmployee_IdAndPeriod(Integer employeeId, LocalDate period);
    List<Payroll> findAllByOrderByPeriodDescEmployee_IdAsc();
}
