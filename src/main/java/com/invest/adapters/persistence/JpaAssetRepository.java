package com.invest.adapters.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaAssetRepository extends JpaRepository<AssetEntity, Long> {

    Optional<AssetEntity> findByTicker(String ticker);

    List<AssetEntity> findByTickerIn(Collection<String> tickers);
}
