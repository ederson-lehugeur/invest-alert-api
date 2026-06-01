package com.invest.application.usecases;

import com.invest.application.ports.in.GetSupportedIndicatorsUseCase;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.InvalidAssetTypeException;
import com.invest.domain.services.AssetTypeIndicatorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetSupportedIndicatorsUseCaseImpl implements GetSupportedIndicatorsUseCase {

    private final AssetTypeIndicatorRegistry indicatorRegistry;

    @Override
    public List<String> execute(String assetTypeValue) {
        log.info("M=execute, I=Consultando indicadores suportados, assetType={}", assetTypeValue);

        AssetType assetType = parseAssetType(assetTypeValue);

        List<String> codes = indicatorRegistry.getSupportedIndicators(assetType).stream()
                .map(IndicatorType::code)
                .sorted()
                .toList();

        log.info("M=execute, I=Indicadores encontrados, assetType={}, count={}", assetTypeValue, codes.size());
        return codes;
    }

    private AssetType parseAssetType(String value) {
        try {
            return AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAssetTypeException(value);
        }
    }
}
