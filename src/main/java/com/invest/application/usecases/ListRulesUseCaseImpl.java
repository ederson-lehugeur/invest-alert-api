package com.invest.application.usecases;

import com.invest.application.ports.in.ListRulesUseCase;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Rule;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListRulesUseCaseImpl implements ListRulesUseCase {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    @Override
    public List<RuleResponse> execute(Long userId) {
        log.info("M=execute, I=Listando regras, userId={}", userId);

        List<RuleResponse> responses = ruleRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();

        log.info("M=execute, I=Regras listadas com sucesso, userId={}, count={}", userId, responses.size());
        return responses;
    }

    private RuleResponse toResponse(Rule rule) {
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
