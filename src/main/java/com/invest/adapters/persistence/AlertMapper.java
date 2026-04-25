package com.invest.adapters.persistence;

import com.invest.domain.entities.Alert;

public final class AlertMapper {

    private AlertMapper() {}

    public static AlertEntity toEntity(Alert domain, UserEntity user,
                                       RuleEntity rule, RuleGroupEntity group) {
        AlertEntity entity = new AlertEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setRule(rule);
        entity.setGroup(group);
        entity.setTicker(domain.getTicker());
        entity.setStatus(domain.getStatus());
        entity.setDetails(domain.getDetails());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setSentAt(domain.getSentAt());
        return entity;
    }

    public static Alert toDomain(AlertEntity entity) {
        Long ruleId = entity.getRule() != null ? entity.getRule().getId() : null;
        Long groupId = entity.getGroup() != null ? entity.getGroup().getId() : null;

        return new Alert(
                entity.getId(),
                entity.getUser().getId(),
                ruleId,
                groupId,
                entity.getTicker(),
                entity.getStatus(),
                entity.getDetails(),
                entity.getCreatedAt(),
                entity.getSentAt()
        );
    }
}
