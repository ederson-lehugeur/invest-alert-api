package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.RuleGroupEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.Rule;

import java.util.List;

public final class RuleGroupMapper {

    private RuleGroupMapper() {}

    public static RuleGroupEntity toEntity(RuleGroup domain, UserEntity user) {
        return RuleGroupEntity.builder()
                .id(domain.getId())
                .user(user)
                .ticker(domain.getTicker())
                .name(domain.getName())
                .createdAt(domain.getCreatedAt())
                .build();
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
