package com.invest.application.commands;

import com.invest.domain.entities.AlertStatus;

public record AlertFilterCommand(String ticker, AlertStatus status) {}
