package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.Customer;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Integer id);
    List<Customer> findByPhoneContaining(String phone);
    Optional<Customer> findByIdAndStoreId(Integer id, Long storeId);
    Optional<Customer> findByStoreIdAndPhone(Long storeId, String phone);
    boolean existsByStoreIdAndPhone(Long storeId, String phone);
    boolean existsByStoreIdAndPhoneAndIdNot(Long storeId, String phone, Integer id);
    List<Customer> findAllByStoreIdOrderByIdAsc(Long storeId);
    List<Customer> findByStoreIdAndPhoneContaining(Long storeId, String phone);
}
