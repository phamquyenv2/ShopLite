package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // --- Active (not deleted) queries by storeMember path ---

    List<Employee> findAllByDeletedFalseOrderByIdAsc();

    Optional<Employee> findByStoreMember_User_IdAndDeletedFalse(Integer userId);

    Optional<Employee> findByQrAndDeletedFalse(String qr);

    boolean existsByStoreMember_User_IdAndDeletedFalse(Integer userId);

    boolean existsByQrAndDeletedFalse(String qr);

    boolean existsByQrAndIdNotAndDeletedFalse(String qr, Integer id);

    boolean existsByStoreMember_User_IdAndIdNotAndDeletedFalse(Integer userId, Integer id);

    // --- Legacy / raw ---

    Optional<Employee> findByStoreMember_User_Id(Integer userId);

    Optional<Employee> findByQr(String qr);

    boolean existsByStoreMember_User_Id(Integer userId);

    boolean existsByQr(String qr);

    boolean existsByQrAndIdNot(String qr, Integer id);

    boolean existsByStoreMember_User_IdAndIdNot(Integer userId, Integer id);

    List<Employee> findAllByOrderByIdAsc();

    // --- Store-scoped ---

    List<Employee> findAllByStoreMember_Store_IdAndDeletedFalseOrderByIdAsc(Long storeId);

    Optional<Employee> findByIdAndStoreMember_Store_IdAndDeletedFalse(Integer id, Long storeId);

    Optional<Employee> findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(Long storeId, Integer userId);

    Optional<Employee> findByStoreMember_Store_IdAndQrAndDeletedFalse(Long storeId, String qr);

    boolean existsByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(Long storeId, Integer userId);

    boolean existsByStoreMember_Store_IdAndQrAndDeletedFalse(Long storeId, String qr);

    boolean existsByStoreMember_Store_IdAndQrAndIdNotAndDeletedFalse(Long storeId, String qr, Integer id);

    boolean existsByStoreMember_Store_IdAndStoreMember_User_IdAndIdNotAndDeletedFalse(Long storeId, Integer userId, Integer id);
}
