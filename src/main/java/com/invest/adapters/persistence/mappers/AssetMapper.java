package com.invest.adapters.persistence.mappers;

import com.invest.adapters.persistence.entities.AssetEntity;
import com.invest.adapters.persistence.entities.AssetIndicatorValueEntity;
import com.invest.adapters.persistence.entities.AssetIndicatorValueId;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.IndicatorValue;
import com.invest.domain.entities.enumerator.IndicatorType;

import java.util.List;

public final class AssetMapper {

    private AssetMapper() {}

    public static Asset toDomain(AssetEntity entity) {
        List<IndicatorValue> indicatorValues = entity.getIndicatorValues().stream()
                .map(iv -> new IndicatorValue(
                        IndicatorType.fromCode(iv.getId().getIndicatorType()).orElseThrow(),
                        iv.getValue()
                ))
                .toList();

        return Asset.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .name(entity.getName())
                .assetType(entity.getAssetType())
                .indicatorValues(indicatorValues)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static AssetEntity toEntity(Asset domain) {
        AssetEntity entity = AssetEntity.builder()
                .id(domain.getId())
                .ticker(domain.getTicker())
                .name(domain.getName())
                .assetType(domain.getAssetType())
                .updatedAt(domain.getUpdatedAt())
                .build();

        List<AssetIndicatorValueEntity> indicatorEntities = domain.getIndicatorValues().stream()
                .map(iv -> AssetIndicatorValueEntity.builder()
                        .id(new AssetIndicatorValueId(entity.getId(), iv.indicatorType().code()))
                        .asset(entity)
                        .value(iv.value())
                        .build())
                .toList();

        entity.setIndicatorValues(indicatorEntities);
        return entity;
    }
}
