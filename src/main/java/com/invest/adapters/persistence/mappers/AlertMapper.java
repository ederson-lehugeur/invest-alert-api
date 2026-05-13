package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.AlertEntity;
import com.invest.adapters.persistence.entities.RuleEntity;
import com.invest.adapters.persistence.entities.RuleGroupEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.domain.entities.Alert;

public final class AlertMapper {

    private AlertMapper() {}

    public static AlertEntity toEntity(Alert domain, UserEntity user,
                                       RuleEntity rule, RuleGroupEntity group) {
        return AlertEntity.builder()
                .id(domain.getId())
                .user(user)
                .rule(rule)
                .group(group)
                .ticker(domain.getTicker())
                .status(domain.getStatus())
                .details(domain.getDetails())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getSentAt())
                .build();
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
