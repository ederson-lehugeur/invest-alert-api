package com.invest.adapters.persistence;

import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AlertRepositoryAdapter implements AlertRepository {

    private final JpaAlertRepository jpaRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaRuleRepository jpaRuleRepository;
    private final JpaRuleGroupRepository jpaRuleGroupRepository;

    public AlertRepositoryAdapter(JpaAlertRepository jpaRepository,
                                  JpaUserRepository jpaUserRepository,
                                  JpaRuleRepository jpaRuleRepository,
                                  JpaRuleGroupRepository jpaRuleGroupRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaRuleRepository = jpaRuleRepository;
        this.jpaRuleGroupRepository = jpaRuleGroupRepository;
    }

    @Override
    public Alert save(Alert alert) {
        UserEntity user = jpaUserRepository.getReferenceById(alert.getUserId());
        RuleEntity rule = alert.getRuleId() != null
                ? jpaRuleRepository.getReferenceById(alert.getRuleId())
                : null;
        RuleGroupEntity group = alert.getGroupId() != null
                ? jpaRuleGroupRepository.getReferenceById(alert.getGroupId())
                : null;

        AlertEntity entity = AlertMapper.toEntity(alert, user, rule, group);
        AlertEntity saved = jpaRepository.save(entity);
        return AlertMapper.toDomain(saved);
    }

    @Override
    public PageResult<Alert> findByUserId(Long userId, PageRequest pageRequest) {
        Page<AlertEntity> page = jpaRepository.findByUserIdOrderByCreatedAtDesc(
                userId, toSpringPageable(pageRequest));
        return toPageResult(page);
    }

    @Override
    public PageResult<Alert> findByUserIdAndTicker(Long userId, String ticker,
                                                   PageRequest pageRequest) {
        Page<AlertEntity> page = jpaRepository.findByUserIdAndTickerOrderByCreatedAtDesc(
                userId, ticker, toSpringPageable(pageRequest));
        return toPageResult(page);
    }

    @Override
    public PageResult<Alert> findByUserIdAndStatus(Long userId, AlertStatus status,
                                                   PageRequest pageRequest) {
        Page<AlertEntity> page = jpaRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, status, toSpringPageable(pageRequest));
        return toPageResult(page);
    }

    @Override
    public PageResult<Alert> findByUserIdTickerAndStatus(Long userId, String ticker,
                                                         AlertStatus status,
                                                         PageRequest pageRequest) {
        Page<AlertEntity> page = jpaRepository
                .findByUserIdAndTickerAndStatusOrderByCreatedAtDesc(
                        userId, ticker, status, toSpringPageable(pageRequest));
        return toPageResult(page);
    }

    @Override
    public List<Alert> findPending() {
        return jpaRepository.findByStatus(AlertStatus.PENDING).stream()
                .map(AlertMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByRuleId(Long ruleId) {
        return jpaRepository.existsByRuleId(ruleId);
    }

    @Override
    public boolean existsActiveAlert(Long ruleId, String ticker) {
        return jpaRepository.existsByRuleIdAndTickerAndStatus(
                ruleId, ticker, AlertStatus.PENDING);
    }

    @Override
    public boolean existsActiveAlertForGroup(Long groupId, String ticker) {
        return jpaRepository.existsByGroupIdAndTickerAndStatus(
                groupId, ticker, AlertStatus.PENDING);
    }

    private PageResult<Alert> toPageResult(Page<AlertEntity> page) {
        List<Alert> content = page.getContent().stream()
                .map(AlertMapper::toDomain)
                .toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private org.springframework.data.domain.Pageable toSpringPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
    }
}
