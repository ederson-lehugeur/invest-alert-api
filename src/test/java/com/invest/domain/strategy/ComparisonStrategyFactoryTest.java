package com.invest.domain.strategy;

import com.invest.domain.entities.ComparisonOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonStrategyFactoryTest {

    @Test
    void criar_returnsMaiorQueStrategy_forMaiorQue() {
        ComparisonStrategy strategy = ComparisonStrategyFactory.create(ComparisonOperator.GREATER_THAN);
        assertInstanceOf(GreaterThanStrategy.class, strategy);
    }

    @Test
    void criar_returnsMenorQueStrategy_forMenorQue() {
        ComparisonStrategy strategy = ComparisonStrategyFactory.create(ComparisonOperator.LESS_THAN);
        assertInstanceOf(LessThanStrategy.class, strategy);
    }

    @Test
    void criar_returnsMaiorOuIgualStrategy_forMaiorOuIgual() {
        ComparisonStrategy strategy = ComparisonStrategyFactory.create(ComparisonOperator.GREATER_THAN_OR_EQUAL);
        assertInstanceOf(GreaterThanOrEqualStrategy.class, strategy);
    }

    @Test
    void criar_returnsMenorOuIgualStrategy_forMenorOuIgual() {
        ComparisonStrategy strategy = ComparisonStrategyFactory.create(ComparisonOperator.LESS_THAN_OR_EQUAL);
        assertInstanceOf(LessThanOrEqualStrategy.class, strategy);
    }

    @Test
    void criar_returnsIgualStrategy_forIgual() {
        ComparisonStrategy strategy = ComparisonStrategyFactory.create(ComparisonOperator.EQUAL);
        assertInstanceOf(EqualStrategy.class, strategy);
    }

    @Test
    void criar_coversAllOperadorComparacaoValues() {
        for (ComparisonOperator operador : ComparisonOperator.values()) {
            ComparisonStrategy strategy = ComparisonStrategyFactory.create(operador);
            assertNotNull(strategy, "Strategy should not be null for " + operador);
        }
    }
}
