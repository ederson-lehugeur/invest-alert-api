package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.RuleField;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Validates: Requirements 9.1
 * Property-based tests for RuleGroup structural/data integrity.
 * After refactoring, RuleGroup is a pure data entity (no evaluate behavior).
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
    Arbitrary<Rule> rules() {
        return Combinators.combine(
                fields(),
                operators(),
                positiveBigDecimals()
        ).as((field, operator, targetValue) ->
                new Rule(1L, 1L, "XPLG11", null, field, operator, targetValue, true, NOW, NOW)
        );
    }

    @Property(tries = 150)
    void groupPreservesAllRules(
            @ForAll @Size(min = 0, max = 10) List<@From("rules") Rule> rulesList) {

        RuleGroup group = new RuleGroup(1L, 1L, "XPLG11", "Test Group", rulesList, NOW);

        assert group.getRules().size() == rulesList.size() :
                "RuleGroup should preserve all %d rules but has %d"
                        .formatted(rulesList.size(), group.getRules().size());

        for (int i = 0; i < rulesList.size(); i++) {
            assert group.getRules().get(i) == rulesList.get(i) :
                    "Rule at index %d should be the same instance".formatted(i);
        }
    }

    @Property(tries = 150)
    void groupPreservesConstructorFields(
            @ForAll("positiveBigDecimals") BigDecimal targetValue,
            @ForAll("fields") RuleField field,
            @ForAll("operators") ComparisonOperator operator) {

        Rule rule = new Rule(1L, 1L, "XPLG11", null, field, operator, targetValue, true, NOW, NOW);
        RuleGroup group = new RuleGroup(42L, 7L, "HGLG11", "My Group", List.of(rule), NOW);

        assert group.getId() == 42L : "id should be preserved";
        assert group.getUserId() == 7L : "userId should be preserved";
        assert "HGLG11".equals(group.getTicker()) : "ticker should be preserved";
        assert "My Group".equals(group.getName()) : "name should be preserved";
        assert group.getCreatedAt().equals(NOW) : "createdAt should be preserved";
        assert group.getRules().size() == 1 : "rules list size should be 1";
    }

    @Property(tries = 100)
    void groupNameIsUpdatableViaSetter(
            @ForAll @Size(min = 1, max = 10) List<@From("rules") Rule> rulesList) {

        RuleGroup group = new RuleGroup(1L, 1L, "XPLG11", "Original", rulesList, NOW);
        group.setName("Updated Name");

        assert "Updated Name".equals(group.getName()) :
                "Name should be updatable via setter";
        assert group.getRules().size() == rulesList.size() :
                "Updating name should not affect rules list";
    }
}
