package com.invest.adapters.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaRuleGroupRepository extends JpaRepository<RuleGroupEntity, Long> {

    @Query("SELECT DISTINCT g FROM RuleGroupEntity g JOIN FETCH g.rules r JOIN FETCH r.asset")
    List<RuleGroupEntity> findAllWithRules();

    List<RuleGroupEntity> findByUserId(Long userId);
}
