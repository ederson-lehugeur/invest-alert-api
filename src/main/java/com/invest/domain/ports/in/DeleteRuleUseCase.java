package com.invest.domain.ports.in;

public interface DeleteRuleUseCase {

    void execute(Long userId, Long ruleId);
}
