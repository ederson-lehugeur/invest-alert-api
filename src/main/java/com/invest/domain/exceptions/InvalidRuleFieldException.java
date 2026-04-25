package com.invest.domain.exceptions;

public class InvalidRuleFieldException extends RuntimeException {

    public InvalidRuleFieldException(String message) {
        super(message);
    }
}
