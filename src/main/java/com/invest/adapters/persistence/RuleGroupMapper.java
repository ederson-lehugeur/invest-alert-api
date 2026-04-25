package com.invest.adapters.persistence;

import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.Rule;

import java.util.List;

public final class RuleGroupMapper {

    private RuleGroupMapper() {}

    public static RuleGroupEntity toEntity(RuleGroup domain, UserEntity user) {
        RuleGroupEntity entity = new RuleGroupEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setTicker(domain.getTicker());
        entity.setName(domain.getName());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static RuleGroup toDomain(RuleGroupEntity entity) {
        List<Rule> rules = entity.getRules().stream()
                .map(RuleMapper::toDomain)
                .toList();

        return new RuleGroup(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTicker(),
                entity.getName(),
                rules,
                entity.getCreatedAt()
        );
    }
}
