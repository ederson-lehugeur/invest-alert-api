package com.invest.application.responses;

import java.util.List;

public record RuleGroupResponse(
        Long id,
        String ticker,
        String name,
        List<RuleResponse> rules
) {}
