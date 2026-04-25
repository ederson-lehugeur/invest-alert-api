package com.invest.domain.strategy;

import com.invest.domain.entities.ComparisonOperator;
import net.jqwik.api.*;

import java.math.BigDecimal;

/**
 * Validates: Requirements 3.3
 * Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
 */
class ComparisonStrategyProperties {

    @Provide
    Arbitrary<BigDecimal> bigDecimals() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(-1_000_000), BigDecimal.valueOf(1_000_000))
                .ofScale(4);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void maiorQueStrategy_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = new GreaterThanStrategy();
        boolean expected = a.compareTo(b) > 0;
        boolean actual = strategy.evaluate(a, b);
        assert actual == expected : "GreaterThanStrategy(%s, %s): expected %s but got %s".formatted(a, b, expected, actual);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void menorQueStrategy_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = new LessThanStrategy();
        boolean expected = a.compareTo(b) < 0;
        boolean actual = strategy.evaluate(a, b);
        assert actual == expected : "LessThanStrategy(%s, %s): expected %s but got %s".formatted(a, b, expected, actual);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void maiorOuIgualStrategy_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = new GreaterThanOrEqualStrategy();
        boolean expected = a.compareTo(b) >= 0;
        boolean actual = strategy.evaluate(a, b);
        assert actual == expected : "GreaterThanOrEqualStrategy(%s, %s): expected %s but got %s".formatted(a, b, expected, actual);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void menorOuIgualStrategy_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = new LessThanOrEqualStrategy();
        boolean expected = a.compareTo(b) <= 0;
        boolean actual = strategy.evaluate(a, b);
        assert actual == expected : "LessThanOrEqualStrategy(%s, %s): expected %s but got %s".formatted(a, b, expected, actual);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void igualStrategy_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = new EqualStrategy();
        boolean expected = a.compareTo(b) == 0;
        boolean actual = strategy.evaluate(a, b);
        assert actual == expected : "EqualStrategy(%s, %s): expected %s but got %s".formatted(a, b, expected, actual);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    // Tests factory wiring: each ComparisonOperator produces the correct strategy result
    @Property(tries = 200)
    void factoryMaiorQue_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = ComparisonStrategyFactory.create(ComparisonOperator.GREATER_THAN);
        assert strategy.evaluate(a, b) == (a.compareTo(b) > 0);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void factoryMenorQue_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = ComparisonStrategyFactory.create(ComparisonOperator.LESS_THAN);
        assert strategy.evaluate(a, b) == (a.compareTo(b) < 0);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void factoryMaiorOuIgual_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = ComparisonStrategyFactory.create(ComparisonOperator.GREATER_THAN_OR_EQUAL);
        assert strategy.evaluate(a, b) == (a.compareTo(b) >= 0);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void factoryMenorOuIgual_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = ComparisonStrategyFactory.create(ComparisonOperator.LESS_THAN_OR_EQUAL);
        assert strategy.evaluate(a, b) == (a.compareTo(b) <= 0);
    }

    // Feature: investments-opportunity-monitor, Property 2: Corretude dos operadores de comparacao
    @Property(tries = 200)
    void factoryIgual_isConsistentWithMathDefinition(
            @ForAll("bigDecimals") BigDecimal a,
            @ForAll("bigDecimals") BigDecimal b) {
        var strategy = ComparisonStrategyFactory.create(ComparisonOperator.EQUAL);
        assert strategy.evaluate(a, b) == (a.compareTo(b) == 0);
    }
}
