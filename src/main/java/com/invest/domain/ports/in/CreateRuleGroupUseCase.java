package com.invest.domain.ports.in;

import com.invest.application.commands.CreateRuleGroupCommand;
import com.invest.application.responses.RuleGroupResponse;

public interface CreateRuleGroupUseCase {

    RuleGroupResponse execute(Long userId, CreateRuleGroupCommand command);
}
