package com.invest.adapters.persistence.repositories;

import com.invest.adapters.persistence.entities.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaRuleRepository extends JpaRepository<RuleEntity, Long> {

    Optional<RuleEntity> findByIdAndUserId(Long id, Long userId);

    List<RuleEntity> findByUserId(Long userId);

    @Query("SELECT r FROM RuleEntity r JOIN FETCH r.asset WHERE r.active = true AND r.group IS NULL")
    List<RuleEntity> findAllActiveWithoutGroup();

    List<RuleEntity> findByGroupId(Long groupId);
}
