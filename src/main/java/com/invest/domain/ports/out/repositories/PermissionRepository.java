package com.invest.domain.ports.out.repositories;

import com.invest.domain.entities.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    Permission save(Permission permission);

    Optional<Permission> findByName(String name);

    List<Permission> findAll();
}
