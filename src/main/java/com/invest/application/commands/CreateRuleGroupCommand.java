package com.invest.application.commands;

import java.util.List;

public record CreateRuleGroupCommand(
        String ticker,
        String name,
        List<CreateRuleCommand> rules
) {}
