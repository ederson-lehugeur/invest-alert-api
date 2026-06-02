package com.invest.domain.exceptions;

public class UnknownIndicatorException extends RuntimeException {

    public UnknownIndicatorException(String code) {
        super("Unknown indicator code: '%s'".formatted(code));
    }
}
