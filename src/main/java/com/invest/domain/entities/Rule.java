package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.IndicatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class Rule {

    @Setter private Long id;
    private Long userId;
    private String ticker;
    @Setter private Long groupId;
    @Setter private IndicatorType indicatorType;
    @Setter private ComparisonOperator operator;
    @Setter private BigDecimal targetValue;
    @Setter private boolean active;
    private LocalDateTime createdAt;
    @Setter private LocalDateTime updatedAt;
}
