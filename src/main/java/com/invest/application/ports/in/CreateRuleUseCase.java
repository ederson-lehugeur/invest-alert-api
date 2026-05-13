package com.invest.application.ports.in;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.responses.RuleResponse;

public interface CreateRuleUseCase {

    RuleResponse execute(Long userId, CreateRuleCommand command);
}
