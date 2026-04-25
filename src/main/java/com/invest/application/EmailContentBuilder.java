package com.invest.application;

import com.invest.domain.entities.Asset;
import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;
import com.invest.domain.entities.RuleGroup;
import com.invest.domain.entities.Rule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailContentBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String buildSubject(Asset asset) {
        log.info("M=buildSubject, I=Construindo assunto do email, ticker={}", asset.getTicker());
        return "Alerta de Investimento - " + asset.getName() + " (" + asset.getTicker() + ")";
    }

    public String buildBody(Asset asset, Rule rule, LocalDateTime avaliacaoTimestamp) {
        log.info("M=buildBody, I=Construindo corpo do email para regra individual, ticker={}, ruleId={}",
                asset.getTicker(), rule != null ? rule.getId() : null);

        StringBuilder body = new StringBuilder();
        appendHeader(body);
        appendAssetDetails(body, asset);
        if (rule != null) {
            appendIndividualRuleCondition(body, rule);
        }
        appendTimestamp(body, avaliacaoTimestamp);
        return body.toString();
    }

    public String buildBody(Asset asset, RuleGroup group, LocalDateTime avaliacaoTimestamp) {
        log.info("M=buildBody, I=Construindo corpo do email para grupo de regras, ticker={}, groupId={}",
                asset.getTicker(), group.getId());

        StringBuilder body = new StringBuilder();
        appendHeader(body);
        appendAssetDetails(body, asset);
        appendGroupRuleConditions(body, group);
        appendTimestamp(body, avaliacaoTimestamp);
        return body.toString();
    }

    private void appendHeader(StringBuilder body) {
        body.append("Alerta de Oportunidade de Investimento\n\n");
    }

    private void appendAssetDetails(StringBuilder body, Asset asset) {
        body.append("Ativo: ").append(asset.getName()).append(" (").append(asset.getTicker()).append(")\n");
        body.append("Preco Atual: R$ ").append(asset.getCurrentPrice()).append("\n");
        body.append("Dividend Yield: ").append(asset.getDividendYield()).append("%\n");
        body.append("P/VP: ").append(asset.getPVp()).append("\n\n");
    }

    private void appendIndividualRuleCondition(StringBuilder body, Rule rule) {
        body.append("Condicao Satisfeita: ");
        body.append(formatRuleCondition(rule));
        body.append("\n\n");
    }

    private void appendGroupRuleConditions(StringBuilder body, RuleGroup group) {
        body.append("Grupo de Regras: ").append(group.getName()).append("\n");
        body.append("Condicoes Satisfeitas:\n");
        for (Rule rule : group.getRules()) {
            body.append("  - ").append(formatRuleCondition(rule)).append("\n");
        }
        body.append("\n");
    }

    private void appendTimestamp(StringBuilder body, LocalDateTime timestamp) {
        body.append("Data/Hora da Avaliacao: ").append(timestamp.format(DATE_FORMATTER)).append("\n");
    }

    private String formatRuleCondition(Rule rule) {
        return formatField(rule.getField()) + " " + formatOperator(rule.getOperator()) + " " + rule.getTargetValue();
    }

    private String formatField(RuleField field) {
        return switch (field) {
            case PRICE -> "Preco";
            case DIVIDEND_YIELD -> "Dividend Yield";
            case P_VP -> "P/VP";
        };
    }

    private String formatOperator(ComparisonOperator operator) {
        return switch (operator) {
            case GREATER_THAN -> ">";
            case LESS_THAN -> "<";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN_OR_EQUAL -> "<=";
            case EQUAL -> "=";
        };
    }
}
