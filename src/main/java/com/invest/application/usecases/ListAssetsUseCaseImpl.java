package com.invest.application.usecases;

import com.invest.application.ports.in.ListAssetsUseCase;
import com.invest.application.responses.AssetResponse;
import com.invest.application.responses.IndicatorValueResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.domain.ports.out.repositories.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListAssetsUseCaseImpl implements ListAssetsUseCase {

    private final AssetRepository assetRepository;

    @Override
    public PageResult<AssetResponse> execute(PageRequest pageRequest) {
        log.info("M=execute, I=Listando ativos, page={}, size={}", pageRequest.page(), pageRequest.size());

        PageResult<Asset> page = assetRepository.findAll(pageRequest);

        var responses = page.content().stream()
                .map(this::toResponse)
                .toList();

        log.info("M=execute, I=Ativos listados com sucesso, totalElements={}", page.totalElements());
        return new PageResult<>(
                responses,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
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
