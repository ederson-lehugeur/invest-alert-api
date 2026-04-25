package com.invest.adapters.persistence;

import com.invest.domain.entities.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaAlertRepository extends JpaRepository<AlertEntity, Long> {

    Page<AlertEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AlertEntity> findByUserIdAndTickerOrderByCreatedAtDesc(Long userId, String ticker, Pageable pageable);

    Page<AlertEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AlertStatus status, Pageable pageable);

    Page<AlertEntity> findByUserIdAndTickerAndStatusOrderByCreatedAtDesc(Long userId, String ticker, AlertStatus status, Pageable pageable);

    List<AlertEntity> findByStatus(AlertStatus status);

    boolean existsByRuleId(Long ruleId);

    boolean existsByRuleIdAndTickerAndStatus(Long ruleId, String ticker, AlertStatus status);

    boolean existsByGroupIdAndTickerAndStatus(Long groupId, String ticker, AlertStatus status);
}
