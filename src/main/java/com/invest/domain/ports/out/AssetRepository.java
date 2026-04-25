package com.invest.domain.ports.out;

import com.invest.domain.entities.Asset;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AssetRepository {

    PageResult<Asset> findAll(PageRequest pageRequest);

    Optional<Asset> findByTicker(String ticker);

    List<Asset> findByTickers(Set<String> tickers);
}
