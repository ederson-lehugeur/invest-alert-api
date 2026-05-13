package com.invest.application.usecases;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.commands.CreateRuleGroupCommand;
import com.invest.application.ports.in.CreateRuleGroupUseCase;
import com.invest.application.responses.RuleGroupResponse;
import com.invest.application.responses.RuleResponse;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.exceptions.InvalidRuleFieldException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.repositories.RuleGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CreateRuleGroupUseCaseImpl implements CreateRuleGroupUseCase {

    private final RuleGroupRepository ruleGroupRepository;
    private final AssetRepository assetRepository;

    @Override
    public RuleGroupResponse execute(Long userId, CreateRuleGroupCommand command) {
        log.info("M=execute, I=Criando grupo de regras, userId={}, ticker={}, name={}", userId, command.ticker(), command.name());

        validateCommand(command);

        assetRepository.findByTicker(command.ticker())
                .orElseThrow(() -> new AssetNotFoundException(command.ticker()));

        validateAllRulesMatchTicker(command);

        LocalDateTime now = LocalDateTime.now();

        List<Rule> rules = command.rules().stream()
                .map(ruleCmd -> toRule(ruleCmd, userId, command.ticker(), now))
                .toList();

        RuleGroup group = new RuleGroup(
                null,
                userId,
                command.ticker(),
                command.name(),
                rules,
                now
        );

        RuleGroup savedGroup = ruleGroupRepository.save(group);
        log.info("M=execute, I=Grupo de regras criado com sucesso, groupId={}, ticker={}", savedGroup.getId(), savedGroup.getTicker());
        return toResponse(savedGroup);
    }

    private void validateCommand(CreateRuleGroupCommand command) {
        if (command.ticker() == null || command.ticker().isBlank()) {
            throw new InvalidRuleFieldException("Field 'ticker' is required");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new InvalidRuleFieldException("Field 'name' is required");
        }
        if (command.rules() == null || command.rules().isEmpty()) {
            throw new InvalidRuleFieldException("At least one rule is required in the group");
        }
        for (CreateRuleCommand rule : command.rules()) {
            validateRuleCommand(rule);
        }
    }

    private void validateRuleCommand(CreateRuleCommand command) {
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

    private void validateAllRulesMatchTicker(CreateRuleGroupCommand command) {
        boolean allMatch = command.rules().stream()
                .allMatch(rule -> rule.ticker() == null || rule.ticker().equals(command.ticker()));
        if (!allMatch) {
            throw new InvalidRuleFieldException(
                    "All rules in a group must reference the same ticker: " + command.ticker());
        }
    }

    private Rule toRule(CreateRuleCommand command, Long userId, String ticker, LocalDateTime now) {
        return new Rule(
                null,
                userId,
                ticker,
                null,
                command.field(),
                command.operator(),
                command.targetValue(),
                true,
                now,
                now
        );
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
