package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.mappers.PermissionMapper;
import com.invest.domain.entities.Permission;
import com.invest.domain.ports.out.repositories.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final JpaPermissionRepository jpaRepository;

    public PermissionRepositoryImpl(JpaPermissionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Permission save(Permission permission) {
        return PermissionMapper.toDomain(jpaRepository.save(PermissionMapper.toEntity(permission)));
    }

    @Override
    public Optional<Permission> findByName(String name) {
        return jpaRepository.findByName(name).map(PermissionMapper::toDomain);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream()
                .map(PermissionMapper::toDomain)
                .collect(Collectors.toList());
    }
}
