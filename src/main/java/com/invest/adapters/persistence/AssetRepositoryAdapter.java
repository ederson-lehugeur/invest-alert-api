package com.invest.adapters.persistence;

import com.invest.domain.entities.Asset;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class AssetRepositoryAdapter implements AssetRepository {

    private final JpaAssetRepository jpaRepository;

    public AssetRepositoryAdapter(JpaAssetRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PageResult<Asset> findAll(PageRequest pageRequest) {
        Page<AssetEntity> page = jpaRepository.findAll(toSpringPageable(pageRequest));
        List<Asset> content = page.getContent().stream()
                .map(AssetMapper::toDomain)
                .toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public Optional<Asset> findByTicker(String ticker) {
        return jpaRepository.findByTicker(ticker).map(AssetMapper::toDomain);
    }

    @Override
    public List<Asset> findByTickers(Set<String> tickers) {
        return jpaRepository.findByTickerIn(tickers).stream()
                .map(AssetMapper::toDomain)
                .toList();
    }

    private org.springframework.data.domain.Pageable toSpringPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
    }
}
