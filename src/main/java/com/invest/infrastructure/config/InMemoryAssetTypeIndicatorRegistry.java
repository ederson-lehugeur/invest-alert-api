package com.invest.infrastructure.config;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.IncompatibleIndicatorException;
import com.invest.domain.services.AssetTypeIndicatorRegistry;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class InMemoryAssetTypeIndicatorRegistry implements AssetTypeIndicatorRegistry {

    private final Map<AssetType, Set<IndicatorType>> configurations;

    public InMemoryAssetTypeIndicatorRegistry() {
        EnumMap<AssetType, Set<IndicatorType>> map = new EnumMap<>(AssetType.class);
        map.put(AssetType.FII, EnumSet.of(
                IndicatorType.PRICE,
                IndicatorType.DIVIDEND_YIELD,
                IndicatorType.PVP
        ));
        map.put(AssetType.STOCK, EnumSet.of(
                IndicatorType.PRICE,
                IndicatorType.DIVIDEND_YIELD,
                IndicatorType.PVP,
                IndicatorType.PL,
                IndicatorType.ROE
        ));
        map.put(AssetType.CRYPTOCURRENCY, EnumSet.of(
                IndicatorType.PRICE
        ));
        this.configurations = Collections.unmodifiableMap(map);
    }

    @Override
    public Set<IndicatorType> getSupportedIndicators(AssetType assetType) {
        return configurations.getOrDefault(assetType, EnumSet.noneOf(IndicatorType.class));
    }

    @Override
    public boolean supports(AssetType assetType, IndicatorType indicatorType) {
        return getSupportedIndicators(assetType).contains(indicatorType);
    }

    @Override
    public Set<IndicatorType> findAll() {
        return EnumSet.allOf(IndicatorType.class);
    }

    @Override
    public Optional<IndicatorType> findByCode(String code) {
        return IndicatorType.fromCode(code);
    }

    @Override
    public void validate(AssetType assetType, IndicatorType indicatorType) {
        if (!supports(assetType, indicatorType)) {
            throw new IncompatibleIndicatorException(assetType, indicatorType,
                    getSupportedIndicators(assetType));
        }
    }
}
