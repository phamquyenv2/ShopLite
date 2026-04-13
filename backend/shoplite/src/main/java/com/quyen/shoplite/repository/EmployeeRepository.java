package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // --- Active (not deleted) queries ---

    /** Tất cả nhân viên đang active */
    List<Employee> findAllByDeletedFalseOrderByIdAsc();

    Optional<Employee> findByUser_IdAndDeletedFalse(Integer userId);

    Optional<Employee> findByQrAndDeletedFalse(String qr);

    boolean existsByUser_IdAndDeletedFalse(Integer userId);

    boolean existsByQrAndDeletedFalse(String qr);

    boolean existsByQrAndIdNotAndDeletedFalse(String qr, Integer id);

    boolean existsByUser_IdAndIdNotAndDeletedFalse(Integer userId, Integer id);

    // --- Legacy / raw queries (used by historical references, e.g. Payroll, Order) ---

    Optional<Employee> findByUser_Id(Integer userId);

    Optional<Employee> findByQr(String qr);

    boolean existsByUser_Id(Integer userId);

    boolean existsByQr(String qr);

    boolean existsByQrAndIdNot(String qr, Integer id);

    boolean existsByUser_IdAndIdNot(Integer userId, Integer id);

    List<Employee> findAllByOrderByIdAsc();
}

