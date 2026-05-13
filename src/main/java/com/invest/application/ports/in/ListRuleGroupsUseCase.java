package com.invest.application.ports.in;

import com.invest.application.responses.RuleGroupResponse;

import java.util.List;

public interface ListRuleGroupsUseCase {

    List<RuleGroupResponse> execute(Long userId);
}
