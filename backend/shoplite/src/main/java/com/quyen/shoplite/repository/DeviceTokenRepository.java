package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.DeviceToken;
import com.quyen.shoplite.domain.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

    List<DeviceToken> findAllByUser(User user);

    List<DeviceToken> findAllByUserId(Integer userId);

    Optional<DeviceToken> findByToken(String token);

    boolean existsByToken(String token);

    void deleteByToken(String token);
}
