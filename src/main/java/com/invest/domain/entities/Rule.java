package com.invest.domain.entities;

import com.invest.domain.strategy.ComparisonStrategy;
import com.invest.domain.strategy.ComparisonStrategyFactory;
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
    @Setter private RuleField field;
    @Setter private ComparisonOperator operator;
    @Setter private BigDecimal targetValue;
    @Setter private boolean active;
    private LocalDateTime createdAt;
    @Setter private LocalDateTime updatedAt;

    public boolean evaluate(Asset asset) {
        BigDecimal currentValue = asset.getValueByField(this.field);
        ComparisonStrategy strategy = ComparisonStrategyFactory.create(this.operator);
        return strategy.evaluate(currentValue, this.targetValue);
    }
}
