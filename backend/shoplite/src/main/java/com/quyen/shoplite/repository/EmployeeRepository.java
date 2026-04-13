package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Optional<Employee> findByUser_Id(Integer userId);

    boolean existsByUser_Id(Integer userId);

    boolean existsByQr(String qr);

    boolean existsByQrAndIdNot(String qr, Integer id);

    boolean existsByUser_IdAndIdNot(Integer userId, Integer id);
}
