package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.mappers.RoleMapper;
import com.invest.domain.entities.Role;
import com.invest.domain.ports.out.repositories.RoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final JpaRoleRepository jpaRepository;

    public RoleRepositoryImpl(JpaRoleRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Role save(Role role) {
        return RoleMapper.toDomain(jpaRepository.save(RoleMapper.toEntity(role)));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepository.findByName(name).map(RoleMapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream()
                .map(RoleMapper::toDomain)
                .collect(Collectors.toList());
    }
}
