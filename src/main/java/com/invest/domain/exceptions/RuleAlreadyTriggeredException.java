package com.invest.domain.exceptions;

public class RuleAlreadyTriggeredException extends RuntimeException {

    public RuleAlreadyTriggeredException(Long ruleId) {
        super("Rule cannot be modified because it has already been triggered (id: " + ruleId + ")");
    }
}
