package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.quyen.shoplite.domain.Permission;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long>,
        JpaSpecificationExecutor<Permission> {

    boolean existsByModuleAndApiPathAndMethod(String module, String apiPath, String method);

    Permission findByNameAndApiPathAndMethod(String name, String apiPath, String method);

    Optional<Permission> findByApiPathAndMethod(String apiPath, String method);
}
