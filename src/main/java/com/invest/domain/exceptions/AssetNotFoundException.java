package com.invest.domain.exceptions;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(String ticker) {
        super("Asset not found with ticker: " + ticker);
    }
}
