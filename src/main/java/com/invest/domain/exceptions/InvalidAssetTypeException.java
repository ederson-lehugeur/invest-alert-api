package com.invest.domain.exceptions;

public class InvalidAssetTypeException extends RuntimeException {

    public InvalidAssetTypeException(String value) {
        super("Invalid asset type: '%s'. Valid values: FII, STOCK, CRYPTOCURRENCY".formatted(value));
    }
}
