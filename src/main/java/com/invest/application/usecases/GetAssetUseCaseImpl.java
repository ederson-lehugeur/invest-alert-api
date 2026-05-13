package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.ports.in.GetAssetUseCase;
import com.invest.domain.ports.out.repositories.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        return new AssetResponse(
                asset.getTicker(),
                asset.getName(),
                asset.getCurrentPrice(),
                asset.getDividendYield(),
                asset.getPVp(),
                asset.getUpdatedAt()
        );
    }
}
