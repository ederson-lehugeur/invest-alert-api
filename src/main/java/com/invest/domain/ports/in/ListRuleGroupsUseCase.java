package com.invest.domain.ports.in;

import com.invest.application.responses.RuleGroupResponse;

import java.util.List;

public interface ListRuleGroupsUseCase {

    List<RuleGroupResponse> execute(Long usuarioId);
}
