package com.invest.application.responses;

import java.math.BigDecimal;

public record IndicatorValueResponse(String code, BigDecimal value) {}
