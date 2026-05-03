package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByCode(String code);

    List<Menu> findAllByActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc();
}
