package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.entities.AssetEntity;
import com.invest.adapters.persistence.entities.RuleEntity;
import com.invest.adapters.persistence.entities.RuleGroupEntity;
import com.invest.adapters.persistence.entities.UserEntity;
import com.invest.adapters.persistence.mappers.RuleMapper;
import com.invest.domain.entities.Rule;
import com.invest.domain.ports.out.repositories.RuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RuleRepositoryImpl implements RuleRepository {

    private final JpaRuleRepository jpaRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaAssetRepository jpaAssetRepository;
    private final JpaRuleGroupRepository jpaRuleGroupRepository;

    public RuleRepositoryImpl(JpaRuleRepository jpaRepository,
                              JpaUserRepository jpaUserRepository,
                              JpaAssetRepository jpaAssetRepository,
                              JpaRuleGroupRepository jpaRuleGroupRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaAssetRepository = jpaAssetRepository;
        this.jpaRuleGroupRepository = jpaRuleGroupRepository;
    }

    @Override
    public Rule save(Rule rule) {
        UserEntity user = jpaUserRepository.getReferenceById(rule.getUserId());
        AssetEntity asset = jpaAssetRepository.findByTicker(rule.getTicker())
                .orElseThrow(() -> new IllegalStateException(
                        "Asset not found for ticker: " + rule.getTicker()));
        RuleGroupEntity group = rule.getGroupId() != null
                ? jpaRuleGroupRepository.getReferenceById(rule.getGroupId())
                : null;

        RuleEntity entity = RuleMapper.toEntity(rule, user, asset, group);
        RuleEntity saved = jpaRepository.save(entity);
        return RuleMapper.toDomain(saved);
    }

    @Override
    public Optional<Rule> findById(Long ruleId) {
        return jpaRepository.findById(ruleId).map(RuleMapper::toDomain);
    }

    @Override
    public Optional<Rule> findByIdAndUserId(Long ruleId, Long userId) {
        return jpaRepository.findByIdAndUserId(ruleId, userId)
                .map(RuleMapper::toDomain);
    }

    @Override
    public List<Rule> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(RuleMapper::toDomain)
                .toList();
    }

    @Override
    public List<Rule> findAllActive() {
        return jpaRepository.findAllActiveWithoutGroup().stream()
                .map(RuleMapper::toDomain)
                .toList();
    }

    @Override
    public List<Rule> findByGroupId(Long groupId) {
        return jpaRepository.findByGroupId(groupId).stream()
                .map(RuleMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Long ruleId) {
        jpaRepository.deleteById(ruleId);
    }
}
