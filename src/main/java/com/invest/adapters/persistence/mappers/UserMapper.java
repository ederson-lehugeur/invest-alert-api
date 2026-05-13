package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.domain.entities.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserEntity toEntity(User domain) {
        return UserEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
