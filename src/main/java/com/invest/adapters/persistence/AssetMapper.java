package com.invest.adapters.persistence;

import com.invest.domain.entities.Asset;

public final class AssetMapper {

    private AssetMapper() {}

    public static AssetEntity toEntity(Asset domain) {
        return new AssetEntity(
                domain.getId(),
                domain.getTicker(),
                domain.getName(),
                domain.getCurrentPrice(),
                domain.getDividendYield(),
                domain.getPVp(),
                domain.getUpdatedAt()
        );
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
