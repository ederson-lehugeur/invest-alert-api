package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.entities.AssetEntity;
import com.invest.adapters.persistence.entities.RuleEntity;
import com.invest.adapters.persistence.entities.RuleGroupEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.adapters.persistence.mappers.RuleGroupMapper;
import com.invest.adapters.persistence.mappers.RuleMapper;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.ports.out.repositories.RuleGroupRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RuleGroupRepositoryImpl implements RuleGroupRepository {

    private final JpaRuleGroupRepository jpaRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaAssetRepository jpaAssetRepository;

    public RuleGroupRepositoryImpl(JpaRuleGroupRepository jpaRepository,
                                   JpaUserRepository jpaUserRepository,
                                   JpaAssetRepository jpaAssetRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaAssetRepository = jpaAssetRepository;
    }

    @Override
    public RuleGroup save(RuleGroup ruleGroup) {
        UserEntity user = jpaUserRepository.getReferenceById(ruleGroup.getUserId());
        RuleGroupEntity entity = RuleGroupMapper.toEntity(ruleGroup, user);

        AssetEntity asset = jpaAssetRepository.findByTicker(ruleGroup.getTicker())
                .orElseThrow(() -> new IllegalStateException(
                        "Asset not found for ticker: " + ruleGroup.getTicker()));

        List<RuleEntity> ruleEntities = ruleGroup.getRules().stream()
                .map(rule -> RuleMapper.toEntity(rule, user, asset, entity))
                .toList();
        entity.setRules(ruleEntities);

        RuleGroupEntity saved = jpaRepository.save(entity);
        return RuleGroupMapper.toDomain(saved);
    }

    @Override
    public List<RuleGroup> findAllWithRules() {
        return jpaRepository.findAllWithRules().stream()
                .map(RuleGroupMapper::toDomain)
                .toList();
    }

    @Override
    public List<RuleGroup> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(RuleGroupMapper::toDomain)
                .toList();
    }
}
