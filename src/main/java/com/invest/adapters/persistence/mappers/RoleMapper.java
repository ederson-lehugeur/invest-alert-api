package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.RoleEntity;
import com.invest.domain.entities.Permission;
import com.invest.domain.entities.Role;

import java.util.Set;
import java.util.stream.Collectors;

public final class RoleMapper {

    private RoleMapper() {}

    public static RoleEntity toEntity(Role domain) {
        Set<com.invest.adapters.persistence.entities.PermissionEntity> permissionEntities =
                domain.getPermissions().stream()
                        .map(PermissionMapper::toEntity)
                        .collect(Collectors.toSet());

        return RoleEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .permissions(permissionEntities)
                .build();
    }

    public static Role toDomain(RoleEntity entity) {
        Set<Permission> permissions = entity.getPermissions().stream()
                .map(PermissionMapper::toDomain)
                .collect(Collectors.toSet());

        return new Role(entity.getId(), entity.getName(), permissions);
    }
}
