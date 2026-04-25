package com.invest.domain.ports.out;

import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;

import java.util.List;

public interface AlertRepository {

    Alert save(Alert alert);

    PageResult<Alert> findByUserId(Long userId, PageRequest pageRequest);

    PageResult<Alert> findByUserIdAndTicker(Long userId, String ticker, PageRequest pageRequest);

    PageResult<Alert> findByUserIdAndStatus(Long userId, AlertStatus status, PageRequest pageRequest);

    PageResult<Alert> findByUserIdTickerAndStatus(Long userId, String ticker, AlertStatus status, PageRequest pageRequest);

    List<Alert> findPending();

    boolean existsByRuleId(Long ruleId);

    boolean existsActiveAlert(Long ruleId, String ticker);

    boolean existsActiveAlertForGroup(Long groupId, String ticker);
}
