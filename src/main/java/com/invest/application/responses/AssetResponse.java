package com.invest.application.responses;

import java.time.LocalDateTime;
import java.util.List;

public record AssetResponse(
        String ticker,
        String name,
        String assetType,
        List<IndicatorValueResponse> indicators,
        LocalDateTime updatedAt
) {}
