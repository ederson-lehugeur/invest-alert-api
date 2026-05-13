package com.invest.application.ports.in;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.responses.RuleResponse;

public interface UpdateRuleUseCase {

    RuleResponse execute(Long userId, Long ruleId, UpdateRuleCommand command);
}
