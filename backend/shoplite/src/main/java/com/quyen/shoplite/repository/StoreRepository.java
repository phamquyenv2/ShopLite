package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
