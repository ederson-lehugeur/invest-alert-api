package com.invest.domain.ports.in;

import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.responses.RuleResponse;

public interface UpdateRuleUseCase {

    RuleResponse execute(Long usuarioId, Long regraId, UpdateRuleCommand command);
}
