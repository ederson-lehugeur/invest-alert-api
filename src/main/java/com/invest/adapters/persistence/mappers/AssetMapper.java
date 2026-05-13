package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.AssetEntity;
import com.invest.domain.entities.Asset;

public final class AssetMapper {

    private AssetMapper() {}

    public static AssetEntity toEntity(Asset domain) {
        return AssetEntity.builder()
                .id(domain.getId())
                .ticker(domain.getTicker())
                .name(domain.getName())
                .currentPrice(domain.getCurrentPrice())
                .dividendYield(domain.getDividendYield())
                .pVp(domain.getPVp())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static Asset toDomain(AssetEntity entity) {
        return new Asset(
                entity.getId(),
                entity.getTicker(),
                entity.getName(),
                entity.getCurrentPrice(),
                entity.getDividendYield(),
                entity.getPVp(),
                entity.getUpdatedAt()
        );
    }
}
