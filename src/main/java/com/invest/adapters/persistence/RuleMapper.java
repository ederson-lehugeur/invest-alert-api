package com.invest.adapters.persistence;

import com.invest.domain.entities.Rule;

public final class RuleMapper {

    private RuleMapper() {}

    public static RuleEntity toEntity(Rule domain, UserEntity user,
                                      AssetEntity asset, RuleGroupEntity group) {
        RuleEntity entity = new RuleEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setAsset(asset);
        entity.setGroup(group);
        entity.setField(domain.getField());
        entity.setOperator(domain.getOperator());
        entity.setTargetValue(domain.getTargetValue());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
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
