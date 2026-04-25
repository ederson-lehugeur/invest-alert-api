package com.invest.domain.exceptions;

public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException() {
        super("Authentication token has expired");
    }
}
