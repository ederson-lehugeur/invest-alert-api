package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Rule;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.ports.in.CreateRuleUseCase;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class CreateRuleUseCaseImpl implements CreateRuleUseCase {

    private final RuleRepository ruleRepository;
    private final AssetRepository assetRepository;

    @Override
    public RuleResponse execute(Long userId, CreateRuleCommand command) {
        log.info("M=execute, I=Criando regra, userId={}, ticker={}, field={}, operator={}", userId, command.ticker(), command.field(), command.operator());

        validateCommand(command);

        assetRepository.findByTicker(command.ticker())
                .orElseThrow(() -> new AssetNotFoundException(command.ticker()));

        LocalDateTime now = LocalDateTime.now();
        Rule rule = new Rule(
                null,
                userId,
                command.ticker(),
                command.groupId(),
                command.field(),
                command.operator(),
                command.targetValue(),
                true,
                now,
                now
        );

        Rule savedRule = ruleRepository.save(rule);
        log.info("M=execute, I=Regra criada com sucesso, ruleId={}, ticker={}", savedRule.getId(), savedRule.getTicker());
        return toResponse(savedRule);
    }

    private void validateCommand(CreateRuleCommand command) {
        if (command.field() == null) {
            throw new InvalidRuleFieldException("Field 'field' is required. Accepted values: PRICE, DIVIDEND_YIELD, P_VP");
        }
        if (command.operator() == null) {
            throw new InvalidRuleFieldException("Field 'operator' is required. Accepted values: GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, EQUAL");
        }
        if (command.targetValue() == null) {
            throw new InvalidRuleFieldException("Field 'targetValue' is required");
        }
        if (command.ticker() == null || command.ticker().isBlank()) {
            throw new InvalidRuleFieldException("Field 'ticker' is required");
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
