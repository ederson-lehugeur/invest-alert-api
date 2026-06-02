package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
@Builder
public class Asset {

    @Setter private Long id;
    private String ticker;
    @Setter private String name;
    @Setter private AssetType assetType;
    @Setter private List<IndicatorValue> indicatorValues;
    @Setter private LocalDateTime updatedAt;

    public Optional<BigDecimal> getValueByIndicator(IndicatorType indicatorType) {
        return indicatorValues.stream()
                .filter(iv -> iv.indicatorType().equals(indicatorType))
                .map(IndicatorValue::value)
                .findFirst();
    }
}
