package com.invest.application.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetResponse(
        String ticker,
        String name,
        BigDecimal currentPrice,
        BigDecimal dividendYield,
        BigDecimal pVp,
        LocalDateTime updatedAt
) {}
