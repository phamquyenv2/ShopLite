package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quyen.shoplite.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
