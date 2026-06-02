package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.ports.in.CreateRuleUseCase;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.exceptions.UnknownIndicatorException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleRepository;
import com.invest.domain.services.AssetTypeIndicatorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class CreateRuleUseCaseImpl implements CreateRuleUseCase {

    private final RuleRepository ruleRepository;
    private final AssetRepository assetRepository;
    private final AssetTypeIndicatorRegistry indicatorRegistry;

    @Override
    public RuleResponse execute(Long userId, CreateRuleCommand command) {
        log.info("M=execute, I=Criando regra, userId={}, ticker={}, indicatorCode={}, operator={}",
                userId, command.ticker(), command.indicatorCode(), command.operator());

        validateCommand(command);

        var asset = assetRepository.findByTicker(command.ticker())
                .orElseThrow(() -> new AssetNotFoundException(command.ticker()));

        IndicatorType indicatorType = indicatorRegistry.findByCode(command.indicatorCode())
                .orElseThrow(() -> new UnknownIndicatorException(command.indicatorCode()));

        indicatorRegistry.validate(asset.getAssetType(), indicatorType);

        LocalDateTime now = LocalDateTime.now();
        Rule rule = new Rule(
                null,
                userId,
                command.ticker(),
                command.groupId(),
                indicatorType,
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
        if (command.indicatorCode() == null || command.indicatorCode().isBlank()) {
            throw new InvalidRuleFieldException("Field 'indicatorCode' is required");
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
                rule.getIndicatorType().code(),
                rule.getOperator(),
                rule.getTargetValue(),
                rule.getGroupId(),
                rule.isActive(),
                false
        );
    }
}
