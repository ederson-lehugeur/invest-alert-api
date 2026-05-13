package com.invest.application.responses;

import com.invest.domain.entities.enumerator.AlertStatus;

import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        String ticker,
        AlertStatus status,
        String details,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {}
