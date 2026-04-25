package com.invest.application.usecases;

import com.invest.application.EmailContentBuilder;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.User;
import com.invest.domain.ports.in.SendPendingAlertsUseCase;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.EmailGateway;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.RuleRepository;
import com.invest.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SendPendingAlertsUseCaseImpl implements SendPendingAlertsUseCase {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final RuleRepository ruleRepository;
    private final RuleGroupRepository ruleGroupRepository;
    private final EmailGateway emailGateway;
    private final EmailContentBuilder emailContentBuilder;

    @Override
    public void execute() {
        log.info("M=execute, I=Iniciando envio de alertas pendentes");

        List<Alert> pendingAlerts = alertRepository.findPending();

        for (Alert alert : pendingAlerts) {
            try {
                sendAlert(alert);
            } catch (Exception e) {
                log.error("M=execute, E=Falha ao enviar alerta, alertId={}, ticker={}", alert.getId(), alert.getTicker(), e);
            }
        }

        log.info("M=execute, I=Envio de alertas pendentes concluido, totalPending={}", pendingAlerts.size());
    }

    private void sendAlert(Alert alert) {
        Optional<User> userOpt = userRepository.findById(alert.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("M=sendAlert, W=Usuario nao encontrado para alerta, alertId={}, userId={}", alert.getId(), alert.getUserId());
            return;
        }

        Optional<Asset> assetOpt = assetRepository.findByTicker(alert.getTicker());
        if (assetOpt.isEmpty()) {
            log.warn("M=sendAlert, W=Ativo nao encontrado para alerta, alertId={}, ticker={}", alert.getId(), alert.getTicker());
            return;
        }

        User user = userOpt.get();
        Asset asset = assetOpt.get();
        String subject = emailContentBuilder.buildSubject(asset);
        String body = buildEmailBody(alert, asset);

        emailGateway.send(user.getEmail(), subject, body);

        alert.markAsSent();
        alertRepository.save(alert);
        log.info("M=sendAlert, I=Alerta enviado com sucesso, alertId={}, ticker={}", alert.getId(), alert.getTicker());

        deactivateAssociatedRules(alert);
    }

    private void deactivateAssociatedRules(Alert alert) {
        if (alert.getRuleId() != null) {
            ruleRepository.findById(alert.getRuleId()).ifPresent(rule -> {
                rule.setActive(false);
                ruleRepository.save(rule);
                log.info("M=deactivateAssociatedRules, I=Regra desativada apos envio de alerta, ruleId={}, alertId={}",
                        rule.getId(), alert.getId());
            });
        }

        if (alert.getGroupId() != null) {
            List<Rule> groupRules = ruleRepository.findByGroupId(alert.getGroupId());
            for (Rule rule : groupRules) {
                rule.setActive(false);
                ruleRepository.save(rule);
            }
            log.info("M=deactivateAssociatedRules, I=Regras do grupo desativadas apos envio de alerta, groupId={}, alertId={}, totalRules={}",
                    alert.getGroupId(), alert.getId(), groupRules.size());
        }
    }

    private String buildEmailBody(Alert alert, Asset asset) {
        if (alert.getRuleId() != null) {
            Optional<Rule> ruleOpt = ruleRepository.findById(alert.getRuleId());
            if (ruleOpt.isPresent()) {
                return emailContentBuilder.buildBody(asset, ruleOpt.get(), alert.getCreatedAt());
            }
        } else if (alert.getGroupId() != null) {
            Optional<RuleGroup> groupOpt = ruleGroupRepository.findAllWithRules().stream()
                    .filter(g -> g.getId().equals(alert.getGroupId()))
                    .findFirst();
            if (groupOpt.isPresent()) {
                return emailContentBuilder.buildBody(asset, groupOpt.get(), alert.getCreatedAt());
            }
        }
        return emailContentBuilder.buildBody(asset, (Rule) null, alert.getCreatedAt());
    }
}
