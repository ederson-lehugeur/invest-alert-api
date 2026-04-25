package com.invest.application.usecases;

import com.invest.domain.entities.Alert;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.Rule;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.ports.in.EvaluateRulesUseCase;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.RuleGroupRepository;
import com.invest.domain.ports.out.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class EvaluateRulesUseCaseImpl implements EvaluateRulesUseCase {

    private final RuleRepository ruleRepository;
    private final RuleGroupRepository ruleGroupRepository;
    private final AssetRepository assetRepository;
    private final AlertRepository alertRepository;

    @Override
    public void execute() {
        log.info("M=execute, I=Iniciando avaliacao de regras");

        List<Rule> individualRules = ruleRepository.findAllActive().stream()
                .filter(rule -> rule.getGroupId() == null)
                .toList();

        List<RuleGroup> groups = ruleGroupRepository.findAllWithRules();

        Map<String, Asset> assetsByTicker = loadAssets(individualRules, groups);

        evaluateIndividualRules(individualRules, assetsByTicker);
        evaluateRuleGroups(groups, assetsByTicker);

        log.info("M=execute, I=Avaliacao de regras concluida, individualRules={}, groups={}", individualRules.size(), groups.size());
    }

    private Map<String, Asset> loadAssets(List<Rule> individualRules, List<RuleGroup> groups) {
        Set<String> tickers = Stream.concat(
                individualRules.stream().map(Rule::getTicker),
                groups.stream().map(RuleGroup::getTicker)
        ).collect(Collectors.toSet());

        if (tickers.isEmpty()) {
            return Map.of();
        }

        return assetRepository.findByTickers(tickers).stream()
                .collect(Collectors.toMap(Asset::getTicker, asset -> asset));
    }

    private void evaluateIndividualRules(List<Rule> rules, Map<String, Asset> assetsByTicker) {
        for (Rule rule : rules) {
            try {
                Asset asset = assetsByTicker.get(rule.getTicker());
                if (asset == null) {
                    log.warn("M=evaluateIndividualRules, W=Ativo nao encontrado para ticker, ticker={}, ruleId={}", rule.getTicker(), rule.getId());
                    continue;
                }

                if (rule.evaluate(asset) && !alertRepository.existsActiveAlert(rule.getId(), rule.getTicker())) {
                    createAlert(rule.getUserId(), rule.getId(), null, rule.getTicker());
                }
            } catch (Exception e) {
                log.error("M=evaluateIndividualRules, E=Erro ao avaliar regra, ruleId={}, ticker={}", rule.getId(), rule.getTicker(), e);
            }
        }
    }

    private void evaluateRuleGroups(List<RuleGroup> groups, Map<String, Asset> assetsByTicker) {
        for (RuleGroup group : groups) {
            try {
                Asset asset = assetsByTicker.get(group.getTicker());
                if (asset == null) {
                    log.warn("M=evaluateRuleGroups, W=Ativo nao encontrado para ticker, ticker={}, groupId={}", group.getTicker(), group.getId());
                    continue;
                }

                if (group.evaluate(asset) && !alertRepository.existsActiveAlertForGroup(group.getId(), group.getTicker())) {
                    createAlert(group.getUserId(), null, group.getId(), group.getTicker());
                }
            } catch (Exception e) {
                log.error("M=evaluateRuleGroups, E=Erro ao avaliar grupo de regras, groupId={}, ticker={}", group.getId(), group.getTicker(), e);
            }
        }
    }

    private void createAlert(Long userId, Long ruleId, Long groupId, String ticker) {
        Alert alert = new Alert(
                null,
                userId,
                ruleId,
                groupId,
                ticker,
                AlertStatus.PENDING,
                null,
                LocalDateTime.now(),
                null
        );
        alertRepository.save(alert);
    }
}
