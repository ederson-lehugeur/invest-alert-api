package com.invest.domain.services;

import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;

import java.util.Optional;
import java.util.Set;

public interface AssetTypeIndicatorRegistry {

    Set<IndicatorType> getSupportedIndicators(AssetType assetType);

    boolean supports(AssetType assetType, IndicatorType indicatorType);

    Set<IndicatorType> findAll();

    Optional<IndicatorType> findByCode(String code);

    void validate(AssetType assetType, IndicatorType indicatorType);
}
