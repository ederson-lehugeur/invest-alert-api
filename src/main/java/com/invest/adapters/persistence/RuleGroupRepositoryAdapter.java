package com.invest.adapters.persistence;

import com.invest.domain.entities.RuleGroup;
import com.invest.domain.ports.out.RuleGroupRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RuleGroupRepositoryAdapter implements RuleGroupRepository {

    private final JpaRuleGroupRepository jpaRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaAssetRepository jpaAssetRepository;

    public RuleGroupRepositoryAdapter(JpaRuleGroupRepository jpaRepository,
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
