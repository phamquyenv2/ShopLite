package com.quyen.shoplite.repository;

import com.quyen.shoplite.util.constant.RegSessionStatus;

import com.quyen.shoplite.domain.RegistrationSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationSessionRepository extends JpaRepository<RegistrationSession, String> {

    Optional<RegistrationSession> findByIdAndStatus(String id, RegSessionStatus status);
}
