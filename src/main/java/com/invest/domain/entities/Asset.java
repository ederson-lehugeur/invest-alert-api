package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.RuleField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class Asset {

    @Setter private Long id;
    private String ticker;
    @Setter private String name;
    @Setter private BigDecimal currentPrice;
    @Setter private BigDecimal dividendYield;
    @Setter private BigDecimal pVp;
    @Setter private LocalDateTime updatedAt;

    public BigDecimal getValueByField(RuleField field) {
        return switch (field) {
            case PRICE -> currentPrice;
            case DIVIDEND_YIELD -> dividendYield;
            case P_VP -> pVp;
        };
    }
}
