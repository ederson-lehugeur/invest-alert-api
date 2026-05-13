package com.invest.application.ports.in;

public interface DeleteRuleUseCase {

    void execute(Long userId, Long ruleId);
}
