package com.invest.application.usecases;

import com.invest.application.responses.RuleGroupResponse;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.Rule;
import com.invest.domain.ports.in.ListRuleGroupsUseCase;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.RuleGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListRuleGroupsUseCaseImpl implements ListRuleGroupsUseCase {

    private final RuleGroupRepository ruleGroupRepository;
    private final AlertRepository alertRepository;

    @Override
    public List<RuleGroupResponse> execute(Long userId) {
        log.info("M=execute, I=Listando grupos de regras, userId={}", userId);

        List<RuleGroup> groups = ruleGroupRepository.findByUserId(userId);

        log.info("M=execute, I=Grupos de regras listados com sucesso, userId={}, count={}", userId, groups.size());
        return groups.stream()
                .map(this::toResponse)
                .toList();
    }

    private RuleGroupResponse toResponse(RuleGroup group) {
        List<RuleResponse> ruleResponses = group.getRules().stream()
                .map(this::toRuleResponse)
                .toList();

        return new RuleGroupResponse(
                group.getId(),
                group.getTicker(),
                group.getName(),
                ruleResponses
        );
    }

    private RuleResponse toRuleResponse(Rule rule) {
        boolean triggered = rule.getGroupId() != null
                ? alertRepository.existsByGroupId(rule.getGroupId())
                : alertRepository.existsByRuleId(rule.getId());

        return new RuleResponse(
                rule.getId(),
                rule.getTicker(),
                rule.getField(),
                rule.getOperator(),
                rule.getTargetValue(),
                rule.getGroupId(),
                rule.isActive(),
                triggered
        );
    }
}
