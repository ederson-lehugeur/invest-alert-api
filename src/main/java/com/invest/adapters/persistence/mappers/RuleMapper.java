package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.AssetEntity;
import com.invest.adapters.persistence.entities.RuleEntity;
import com.invest.adapters.persistence.entities.RuleGroupEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.domain.entities.Rule;

public final class RuleMapper {

    private RuleMapper() {}

    public static RuleEntity toEntity(Rule domain, UserEntity user,
                                      AssetEntity asset, RuleGroupEntity group) {
        return RuleEntity.builder()
                .id(domain.getId())
                .user(user)
                .asset(asset)
                .group(group)
                .field(domain.getField())
                .operator(domain.getOperator())
                .targetValue(domain.getTargetValue())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static Rule toDomain(RuleEntity entity) {
        Long groupId = entity.getGroup() != null ? entity.getGroup().getId() : null;
        return new Rule(
                entity.getId(),
                entity.getUser().getId(),
                entity.getAsset().getTicker(),
                groupId,
                entity.getField(),
                entity.getOperator(),
                entity.getTargetValue(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
