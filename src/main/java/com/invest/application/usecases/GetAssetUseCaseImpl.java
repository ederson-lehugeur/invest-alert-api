package com.invest.application.usecases;

import com.invest.application.ports.in.GetAssetUseCase;
import com.invest.application.responses.AssetResponse;
import com.invest.application.responses.IndicatorValueResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetAssetUseCaseImpl implements GetAssetUseCase {

    private final AssetRepository assetRepository;

    @Override
    public AssetResponse execute(String ticker) {
        log.info("M=execute, I=Consultando ativo, ticker={}", ticker);

        var asset = assetRepository.findByTicker(ticker)
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Ativo nao encontrado, ticker={}", ticker);
                    return new AssetNotFoundException(ticker);
                });

        log.info("M=execute, I=Ativo encontrado, ticker={}, name={}", asset.getTicker(), asset.getName());
        return toResponse(asset);
    }

    private AssetResponse toResponse(Asset asset) {
        List<IndicatorValueResponse> indicators = asset.getIndicatorValues().stream()
                .map(iv -> new IndicatorValueResponse(iv.indicatorType().code(), iv.value()))
                .toList();

        return new AssetResponse(
                asset.getTicker(),
                asset.getName(),
                asset.getAssetType().name(),
                indicators,
                asset.getUpdatedAt()
        );
    }
}
