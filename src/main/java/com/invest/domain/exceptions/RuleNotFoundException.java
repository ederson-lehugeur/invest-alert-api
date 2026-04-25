package com.invest.domain.exceptions;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(Long ruleId) {
        super("Rule not found with id: " + ruleId);
    }
}
