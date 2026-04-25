package com.invest.domain.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonStrategyTest {

    @Test
    void maiorQueStrategy_returnsTrue_whenValorAtualIsGreater() {
        var strategy = new GreaterThanStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("10.5"), new BigDecimal("5.0")));
    }

    @Test
    void maiorQueStrategy_returnsFalse_whenValorAtualIsEqual() {
        var strategy = new GreaterThanStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("5.0"), new BigDecimal("5.0")));
    }

    @Test
    void maiorQueStrategy_returnsFalse_whenValorAtualIsLess() {
        var strategy = new GreaterThanStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("3.0"), new BigDecimal("5.0")));
    }

    @Test
    void menorQueStrategy_returnsTrue_whenValorAtualIsLess() {
        var strategy = new LessThanStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("3.0"), new BigDecimal("5.0")));
    }

    @Test
    void menorQueStrategy_returnsFalse_whenValorAtualIsEqual() {
        var strategy = new LessThanStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("5.0"), new BigDecimal("5.0")));
    }

    @Test
    void menorQueStrategy_returnsFalse_whenValorAtualIsGreater() {
        var strategy = new LessThanStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("10.0"), new BigDecimal("5.0")));
    }

    @Test
    void maiorOuIgualStrategy_returnsTrue_whenValorAtualIsGreater() {
        var strategy = new GreaterThanOrEqualStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("10.0"), new BigDecimal("5.0")));
    }

    @Test
    void maiorOuIgualStrategy_returnsTrue_whenValorAtualIsEqual() {
        var strategy = new GreaterThanOrEqualStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("5.0"), new BigDecimal("5.0")));
    }

    @Test
    void maiorOuIgualStrategy_returnsFalse_whenValorAtualIsLess() {
        var strategy = new GreaterThanOrEqualStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("3.0"), new BigDecimal("5.0")));
    }

    @Test
    void menorOuIgualStrategy_returnsTrue_whenValorAtualIsLess() {
        var strategy = new LessThanOrEqualStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("3.0"), new BigDecimal("5.0")));
    }

    @Test
    void menorOuIgualStrategy_returnsTrue_whenValorAtualIsEqual() {
        var strategy = new LessThanOrEqualStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("5.0"), new BigDecimal("5.0")));
    }

    @Test
    void menorOuIgualStrategy_returnsFalse_whenValorAtualIsGreater() {
        var strategy = new LessThanOrEqualStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("10.0"), new BigDecimal("5.0")));
    }

    @Test
    void igualStrategy_returnsTrue_whenValuesAreEqual() {
        var strategy = new EqualStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("5.0"), new BigDecimal("5.0")));
    }

    @Test
    void igualStrategy_returnsFalse_whenValuesAreDifferent() {
        var strategy = new EqualStrategy();
        assertFalse(strategy.evaluate(new BigDecimal("5.1"), new BigDecimal("5.0")));
    }

    @Test
    void igualStrategy_returnsTrue_whenDifferentScaleButSameValue() {
        var strategy = new EqualStrategy();
        assertTrue(strategy.evaluate(new BigDecimal("5.00"), new BigDecimal("5.0")));
    }
}
