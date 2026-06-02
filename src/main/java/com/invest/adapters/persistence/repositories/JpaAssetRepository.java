package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.entities.AssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaAssetRepository extends JpaRepository<AssetEntity, Long> {

    @Query("SELECT a FROM AssetEntity a LEFT JOIN FETCH a.indicatorValues WHERE a.ticker = :ticker")
    Optional<AssetEntity> findByTicker(@Param("ticker") String ticker);

    @Query("SELECT DISTINCT a FROM AssetEntity a LEFT JOIN FETCH a.indicatorValues WHERE a.ticker IN :tickers")
    List<AssetEntity> findByTickerIn(@Param("tickers") Collection<String> tickers);

    @EntityGraph(attributePaths = "indicatorValues")
    Page<AssetEntity> findAll(Pageable pageable);
}
