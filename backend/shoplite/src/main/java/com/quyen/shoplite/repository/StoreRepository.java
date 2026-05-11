package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quyen.shoplite.domain.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
