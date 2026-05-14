package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.RoleEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.domain.entities.Role;
import com.invest.domain.entities.User;
import com.invest.domain.entities.enumerator.SubscriptionPlan;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserEntity toEntity(User domain) {
        Set<RoleEntity> roleEntities = domain.getRoles().stream()
                .map(RoleMapper::toEntity)
                .collect(Collectors.toSet());

        return UserEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .subscriptionPlan(domain.getSubscriptionPlan() != null
                        ? domain.getSubscriptionPlan()
                        : SubscriptionPlan.FREE)
                .enabled(domain.isEnabled())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .roles(roleEntities)
                .build();
    }

    public static User toDomain(UserEntity entity) {
        Set<Role> roles = entity.getRoles().stream()
                .map(RoleMapper::toDomain)
                .collect(Collectors.toSet());

        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSubscriptionPlan(),
                entity.isEnabled(),
                roles
        );
    }
}
