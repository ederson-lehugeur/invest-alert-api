package com.invest.domain.entities;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Validates: Requirements 4.2
 * Feature: investments-opportunity-monitor, Property 4: Avaliacao AND de grupo de regras
 */
class RuleGroupProperties {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Provide
    Arbitrary<BigDecimal> positiveBigDecimals() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(1_000_000))
                .ofScale(4);
    }

    @Provide
    Arbitrary<RuleField> fields() {
        return Arbitraries.of(RuleField.values());
    }

    @Provide
    Arbitrary<ComparisonOperator> operators() {
        return Arbitraries.of(ComparisonOperator.values());
    }

    @Provide
    Arbitrary<Asset> assets() {
        return Combinators.combine(
                positiveBigDecimals(),
                positiveBigDecimals(),
                positiveBigDecimals()
        ).as((price, dy, pvp) ->
                new Asset(1L, "XPLG11", "FII XP Log", price, dy, pvp, NOW)
        );
    }

    @Provide
    Arbitrary<Rule> rules() {
        return Combinators.combine(
                fields(),
                operators(),
                positiveBigDecimals()
        ).as((field, operator, targetValue) ->
                new Rule(1L, 1L, "XPLG11", null, field, operator, targetValue, true, NOW, NOW)
        );
    }

    // Feature: investments-opportunity-monitor, Property 4: Avaliacao AND de grupo de regras
    @Property(tries = 150)
    void groupEvaluate_returnsTrueIffAllRulesReturnTrue(
            @ForAll("assets") Asset asset,
            @ForAll @Size(min = 1, max = 10) List<@From("rules") Rule> rulesList) {

        RuleGroup group = new RuleGroup(1L, 1L, "XPLG11", "Test Group", rulesList, NOW);

        boolean allIndividuallyTrue = rulesList.stream()
                .allMatch(rule -> rule.evaluate(asset));

        boolean groupResult = group.evaluate(asset);

        assert groupResult == allIndividuallyTrue :
                "RuleGroup.evaluate() returned %s but individual allMatch was %s for %d rules"
                        .formatted(groupResult, allIndividuallyTrue, rulesList.size());
    }

    // Feature: investments-opportunity-monitor, Property 4: Avaliacao AND de grupo de regras
    @Property(tries = 100)
    void emptyGroup_alwaysReturnsTrue(@ForAll("assets") Asset asset) {
        RuleGroup group = new RuleGroup(1L, 1L, "XPLG11", "Empty Group", List.of(), NOW);

        boolean result = group.evaluate(asset);

        assert result : "Empty RuleGroup should return true (allMatch on empty stream is true)";
    }

    // Feature: investments-opportunity-monitor, Property 4: Avaliacao AND de grupo de regras
    @Property(tries = 150)
    void singleRuleGroup_matchesIndividualRuleResult(
            @ForAll("assets") Asset asset,
            @ForAll("rules") Rule rule) {

        RuleGroup group = new RuleGroup(1L, 1L, "XPLG11", "Single Rule Group", List.of(rule), NOW);

        boolean individualResult = rule.evaluate(asset);
        boolean groupResult = group.evaluate(asset);

        assert groupResult == individualResult :
                "Single-rule group returned %s but individual rule returned %s"
                        .formatted(groupResult, individualResult);
    }
}
