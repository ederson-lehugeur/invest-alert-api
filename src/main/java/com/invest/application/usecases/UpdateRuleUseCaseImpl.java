package com.invest.application.usecases;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.ports.in.UpdateRuleUseCase;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Rule;
import com.invest.domain.exceptions.AccessDeniedException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.exceptions.RuleAlreadyTriggeredException;
import com.invest.domain.exceptions.RuleNotFoundException;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class UpdateRuleUseCaseImpl implements UpdateRuleUseCase {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    @Override
    public RuleResponse execute(Long userId, Long ruleId, UpdateRuleCommand command) {
        log.info("M=execute, I=Atualizando regra, userId={}, ruleId={}, field={}, operator={}", userId, ruleId, command.field(), command.operator());

        validateCommand(command);

        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Regra nao encontrada, ruleId={}", ruleId);
                    return new RuleNotFoundException(ruleId);
                });

        if (!rule.getUserId().equals(userId)) {
            log.warn("M=execute, W=Acesso negado, ruleId={}, userId={}", ruleId, userId);
            throw new AccessDeniedException("Access denied: rule does not belong to the authenticated user");
        }

        if (alertRepository.existsByRuleId(ruleId)) {
            log.warn("M=execute, W=Regra ja acionada, ruleId={}", ruleId);
            throw new RuleAlreadyTriggeredException(ruleId);
        }

        rule.setField(command.field());
        rule.setOperator(command.operator());
        rule.setTargetValue(command.targetValue());
        rule.setUpdatedAt(LocalDateTime.now());

        Rule updatedRule = ruleRepository.save(rule);
        log.info("M=execute, I=Regra atualizada com sucesso, ruleId={}", updatedRule.getId());
        return toResponse(updatedRule);
    }

    private void validateCommand(UpdateRuleCommand command) {
        if (command.field() == null) {
            throw new InvalidRuleFieldException("Field 'field' is required. Accepted values: PRICE, DIVIDEND_YIELD, P_VP");
        }
        if (command.operator() == null) {
            throw new InvalidRuleFieldException("Field 'operator' is required. Accepted values: GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, EQUAL");
        }
        if (command.targetValue() == null) {
            throw new InvalidRuleFieldException("Field 'targetValue' is required");
        }
    }

    private RuleResponse toResponse(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getTicker(),
                rule.getField(),
                rule.getOperator(),
                rule.getTargetValue(),
                rule.getGroupId(),
                rule.isActive(),
                false
        );
    }
}
