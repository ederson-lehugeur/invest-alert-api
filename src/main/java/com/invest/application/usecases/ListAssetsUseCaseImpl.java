package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.ports.in.ListAssetsUseCase;
import com.invest.domain.ports.out.repositories.AssetRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
