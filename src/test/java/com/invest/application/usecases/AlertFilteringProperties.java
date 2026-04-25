package com.invest.application.usecases;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.application.responses.AlertResponse;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Validates: Requirements 7.2, 7.3
 * Feature: investments-opportunity-monitor, Property 8: Corretude da filtragem de alertas
 */
class AlertFilteringProperties {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final String[] TICKERS = {"XPLG11", "HGLG11", "MXRF11", "KNRI11", "VISC11"};

    @Provide
    Arbitrary<AlertStatus> statuses() {
        return Arbitraries.of(AlertStatus.values());
    }

    @Provide
    Arbitrary<String> tickers() {
        return Arbitraries.of(TICKERS);
    }

    @Provide
    Arbitrary<List<Alert>> mixedAlerts() {
        Arbitrary<Integer> countArb = Arbitraries.integers().between(2, 30);
        return countArb.flatMap(count -> {
            List<Arbitrary<Alert>> alertArbitraries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final int index = i;
                Arbitrary<Alert> alertArb = Combinators.combine(
                        Arbitraries.of(TICKERS),
                        Arbitraries.of(AlertStatus.values()),
                        Arbitraries.integers().between(0, 525_600)
                ).as((ticker, status, minuteOffset) -> {
                    LocalDateTime createdAt = BASE_TIME.plusMinutes(minuteOffset);
                    LocalDateTime sentAt = status == AlertStatus.SENT ? createdAt.plusMinutes(5) : null;
                    return new Alert(
                            (long) (index + 1), USER_ID, (long) (index + 1), null,
                            ticker, status, "Details " + index,
                            createdAt, sentAt
                    );
                });
                alertArbitraries.add(alertArb);
            }
            return Combinators.combine(alertArbitraries).as(list -> list);
        });
    }

    // Feature: investments-opportunity-monitor, Property 8: Corretude da filtragem de alertas
    @Property(tries = 200)
    void filterByTicker_returnsOnlyAlertsWithSpecifiedTicker(
            @ForAll("mixedAlerts") List<Alert> alerts,
            @ForAll("tickers") String targetTicker) {

        AlertRepository repository = createStubRepository(alerts);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(targetTicker, null);
        PageRequest pageRequest = new PageRequest(0, alerts.size() + 1);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        boolean allMatchTicker = result.content().stream()
                .allMatch(response -> response.ticker().equals(targetTicker));

        assert allMatchTicker :
                "Filter by ticker '%s' returned alerts with different tickers".formatted(targetTicker);

        long expectedCount = alerts.stream()
                .filter(a -> a.getTicker().equals(targetTicker))
                .count();

        assert result.content().size() == expectedCount :
                "Expected %d alerts for ticker '%s' but got %d"
                        .formatted(expectedCount, targetTicker, result.content().size());
    }

    // Feature: investments-opportunity-monitor, Property 8: Corretude da filtragem de alertas
    @Property(tries = 200)
    void filterByStatus_returnsOnlyAlertsWithSpecifiedStatus(
            @ForAll("mixedAlerts") List<Alert> alerts,
            @ForAll("statuses") AlertStatus targetStatus) {

        AlertRepository repository = createStubRepository(alerts);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(null, targetStatus);
        PageRequest pageRequest = new PageRequest(0, alerts.size() + 1);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        boolean allMatchStatus = result.content().stream()
                .allMatch(response -> response.status() == targetStatus);

        assert allMatchStatus :
                "Filter by status '%s' returned alerts with different statuses".formatted(targetStatus);

        long expectedCount = alerts.stream()
                .filter(a -> a.getStatus() == targetStatus)
                .count();

        assert result.content().size() == expectedCount :
                "Expected %d alerts with status '%s' but got %d"
                        .formatted(expectedCount, targetStatus, result.content().size());
    }

    // Feature: investments-opportunity-monitor, Property 8: Corretude da filtragem de alertas
    @Property(tries = 200)
    void filterByTickerAndStatus_returnsOnlyAlertsMatchingBothCriteria(
            @ForAll("mixedAlerts") List<Alert> alerts,
            @ForAll("tickers") String targetTicker,
            @ForAll("statuses") AlertStatus targetStatus) {

        AlertRepository repository = createStubRepository(alerts);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(targetTicker, targetStatus);
        PageRequest pageRequest = new PageRequest(0, alerts.size() + 1);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        boolean allMatchBoth = result.content().stream()
                .allMatch(response ->
                        response.ticker().equals(targetTicker) && response.status() == targetStatus);

        assert allMatchBoth :
                "Filter by ticker '%s' and status '%s' returned non-matching alerts"
                        .formatted(targetTicker, targetStatus);

        long expectedCount = alerts.stream()
                .filter(a -> a.getTicker().equals(targetTicker) && a.getStatus() == targetStatus)
                .count();

        assert result.content().size() == expectedCount :
                "Expected %d alerts for ticker '%s' with status '%s' but got %d"
                        .formatted(expectedCount, targetTicker, targetStatus, result.content().size());
    }

    // Feature: investments-opportunity-monitor, Property 8: Corretude da filtragem de alertas
    @Property(tries = 200)
    void noFilter_returnsAllUserAlerts(
            @ForAll("mixedAlerts") List<Alert> alerts) {

        AlertRepository repository = createStubRepository(alerts);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        PageRequest pageRequest = new PageRequest(0, alerts.size() + 1);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        assert result.content().size() == alerts.size() :
                "No filter should return all %d alerts but got %d"
                        .formatted(alerts.size(), result.content().size());
    }

    // Feature: investments-opportunity-monitor, Property 8: Corretude da filtragem de alertas
    @Property(tries = 200)
    void filterByTicker_doesNotReturnAlertsFromOtherTickers(
            @ForAll("mixedAlerts") List<Alert> alerts,
            @ForAll("tickers") String targetTicker) {

        AlertRepository repository = createStubRepository(alerts);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(targetTicker, null);
        PageRequest pageRequest = new PageRequest(0, alerts.size() + 1);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        Set<Long> returnedIds = new HashSet<>();
        result.content().forEach(r -> returnedIds.add(r.id()));

        boolean noExcludedAlerts = alerts.stream()
                .filter(a -> !a.getTicker().equals(targetTicker))
                .noneMatch(a -> returnedIds.contains(a.getId()));

        assert noExcludedAlerts :
                "Filter by ticker '%s' incorrectly included alerts from other tickers"
                        .formatted(targetTicker);
    }

    private AlertRepository createStubRepository(List<Alert> allAlerts) {
        return new AlertRepository() {
            @Override
            public Alert save(Alert alert) { return alert; }

            @Override
            public PageResult<Alert> findByUserId(Long userId, PageRequest pageRequest) {
                List<Alert> filtered = allAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId))
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public PageResult<Alert> findByUserIdAndTicker(Long userId, String ticker, PageRequest pageRequest) {
                List<Alert> filtered = allAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId) && a.getTicker().equals(ticker))
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public PageResult<Alert> findByUserIdAndStatus(Long userId, AlertStatus status, PageRequest pageRequest) {
                List<Alert> filtered = allAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId) && a.getStatus() == status)
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public PageResult<Alert> findByUserIdTickerAndStatus(Long userId, String ticker, AlertStatus status, PageRequest pageRequest) {
                List<Alert> filtered = allAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId) && a.getTicker().equals(ticker) && a.getStatus() == status)
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public List<Alert> findPending() {
                return allAlerts.stream().filter(a -> a.getStatus() == AlertStatus.PENDING).toList();
            }

            @Override
            public boolean existsActiveAlert(Long ruleId, String ticker) { return false; }

            @Override
            public boolean existsActiveAlertForGroup(Long groupId, String ticker) { return false; }

            @Override
            public boolean existsByRuleId(Long ruleId) { return false; }

            private PageResult<Alert> toPageResult(List<Alert> items, PageRequest pageRequest) {
                int start = pageRequest.page() * pageRequest.size();
                int end = Math.min(start + pageRequest.size(), items.size());
                List<Alert> page = start < items.size() ? items.subList(start, end) : List.of();
                int totalPages = (int) Math.ceil((double) items.size() / pageRequest.size());
                return new PageResult<>(page, pageRequest.page(), pageRequest.size(), items.size(), totalPages);
            }
        };
    }
}
