package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Integer id);
    List<Customer> findByPhoneContaining(String phone);
}
