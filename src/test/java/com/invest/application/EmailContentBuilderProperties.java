package com.invest.application;

import com.invest.domain.entities.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Validates: Requirement 6.5
 * Feature: investments-opportunity-monitor, Property 6: Completude do corpo do e-mail de alerta
 */
class EmailContentBuilderProperties {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailContentBuilder builder = new EmailContentBuilder();

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
    Arbitrary<String> tickers() {
        return Arbitraries.of("XPLG11", "HGLG11", "MXRF11", "KNRI11", "VISC11");
    }

    @Provide
    Arbitrary<String> names() {
        return Arbitraries.of("FII XP Log", "FII CSHG Log", "Maxi Renda", "Kinea Renda", "Vinci Shopping");
    }

    @Provide
    Arbitrary<Asset> assets() {
        return Combinators.combine(
                tickers(),
                names(),
                positiveBigDecimals(),
                positiveBigDecimals(),
                positiveBigDecimals()
        ).as((ticker, name, price, dy, pvp) ->
                new Asset(1L, ticker, name, price, dy, pvp, NOW)
        );
    }

    @Provide
    Arbitrary<Rule> rules() {
        return Combinators.combine(
                tickers(),
                fields(),
                operators(),
                positiveBigDecimals()
        ).as((ticker, field, operator, targetValue) ->
                new Rule(1L, 1L, ticker, null, field, operator, targetValue, true, NOW, NOW)
        );
    }

    @Provide
    Arbitrary<LocalDateTime> timestamps() {
        return Arbitraries.longs()
                .between(1_000_000_000L, 2_000_000_000L)
                .map(epoch -> LocalDateTime.of(2024, 1, 1, 0, 0).plusSeconds(epoch % 31_536_000));
    }

    // Feature: investments-opportunity-monitor, Property 6: Completude do corpo do e-mail de alerta
    @Property(tries = 200)
    void individualRuleEmailBody_containsAllRequiredFields(
            @ForAll("assets") Asset asset,
            @ForAll("rules") Rule rule,
            @ForAll("timestamps") LocalDateTime timestamp) {

        String body = builder.buildBody(asset, rule, timestamp);

        assertContainsAssetName(body, asset);
        assertContainsTicker(body, asset);
        assertContainsCurrentPrice(body, asset);
        assertContainsDividendYield(body, asset);
        assertContainsPVP(body, asset);
        assertContainsRuleCondition(body, rule);
        assertContainsTimestamp(body, timestamp);
    }

    // Feature: investments-opportunity-monitor, Property 6: Completude do corpo do e-mail de alerta
    @Property(tries = 200)
    void groupRuleEmailBody_containsAllRequiredFields(
            @ForAll("assets") Asset asset,
            @ForAll @net.jqwik.api.constraints.Size(min = 1, max = 5) List<@From("rules") Rule> rulesList,
            @ForAll("timestamps") LocalDateTime timestamp) {

        RuleGroup group = new RuleGroup(1L, 1L, asset.getTicker(), "Test Group", rulesList, NOW);

        String body = builder.buildBody(asset, group, timestamp);

        assertContainsAssetName(body, asset);
        assertContainsTicker(body, asset);
        assertContainsCurrentPrice(body, asset);
        assertContainsDividendYield(body, asset);
        assertContainsPVP(body, asset);
        for (Rule rule : rulesList) {
            assertContainsRuleCondition(body, rule);
        }
        assertContainsTimestamp(body, timestamp);
    }

    // Feature: investments-opportunity-monitor, Property 6: Completude do corpo do e-mail de alerta
    @Property(tries = 150)
    void emailSubject_containsAssetNameAndTicker(
            @ForAll("assets") Asset asset) {

        String subject = builder.buildSubject(asset);

        assert subject.contains(asset.getName()) :
                "Subject must contain asset name '%s', got: '%s'".formatted(asset.getName(), subject);
        assert subject.contains(asset.getTicker()) :
                "Subject must contain ticker '%s', got: '%s'".formatted(asset.getTicker(), subject);
    }

    private void assertContainsAssetName(String body, Asset asset) {
        assert body.contains(asset.getName()) :
                "Email body must contain asset name '%s'".formatted(asset.getName());
    }

    private void assertContainsTicker(String body, Asset asset) {
        assert body.contains(asset.getTicker()) :
                "Email body must contain ticker '%s'".formatted(asset.getTicker());
    }

    private void assertContainsCurrentPrice(String body, Asset asset) {
        assert body.contains(asset.getCurrentPrice().toString()) :
                "Email body must contain current price '%s'".formatted(asset.getCurrentPrice());
    }

    private void assertContainsDividendYield(String body, Asset asset) {
        assert body.contains(asset.getDividendYield().toString()) :
                "Email body must contain Dividend Yield '%s'".formatted(asset.getDividendYield());
    }

    private void assertContainsPVP(String body, Asset asset) {
        assert body.contains(asset.getPVp().toString()) :
                "Email body must contain P/VP '%s'".formatted(asset.getPVp());
    }

    private void assertContainsRuleCondition(String body, Rule rule) {
        assert body.contains(rule.getTargetValue().toString()) :
                "Email body must contain rule target value '%s'".formatted(rule.getTargetValue());
    }

    private void assertContainsTimestamp(String body, LocalDateTime timestamp) {
        String formatted = timestamp.format(DATE_FORMATTER);
        assert body.contains(formatted) :
                "Email body must contain evaluation timestamp '%s'".formatted(formatted);
    }
}
