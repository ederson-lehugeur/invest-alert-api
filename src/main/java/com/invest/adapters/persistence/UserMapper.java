package com.invest.adapters.persistence;

import com.invest.domain.entities.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserEntity toEntity(User domain) {
        return new UserEntity(
                domain.getId(),
                domain.getName(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
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
