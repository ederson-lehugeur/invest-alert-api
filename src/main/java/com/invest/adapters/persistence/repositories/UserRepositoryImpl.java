package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.entities.RoleEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.adapters.persistence.mappers.UserMapper;
import com.invest.domain.entities.User;
import com.invest.domain.ports.out.repositories.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaRepository;
    private final JpaRoleRepository jpaRoleRepository;

    public UserRepositoryImpl(JpaUserRepository jpaRepository, JpaRoleRepository jpaRoleRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaRoleRepository = jpaRoleRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);

        Set<Long> roleIds = user.getRoles().stream()
                .map(com.invest.domain.entities.Role::getId)
                .collect(Collectors.toSet());

        List<RoleEntity> managedRoles = jpaRoleRepository.findAllById(roleIds);
        entity.setRoles(new java.util.HashSet<>(managedRoles));

        UserEntity saved = jpaRepository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
