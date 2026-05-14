package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.PermissionEntity;
import com.invest.domain.entities.Permission;

public final class PermissionMapper {

    private PermissionMapper() {}

    public static PermissionEntity toEntity(Permission domain) {
        return PermissionEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .build();
    }

    public static Permission toDomain(PermissionEntity entity) {
        return new Permission(entity.getId(), entity.getName());
    }
}
