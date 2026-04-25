package com.invest.application.usecases;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.application.responses.AlertResponse;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.AlertStatus;
import com.invest.domain.ports.out.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Validates: Requirement 7.1
 * Feature: investments-opportunity-monitor, Property 7: Ordenacao do historico de alertas
 */
class AlertOrderingProperties {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 0, 0);

    @Provide
    Arbitrary<AlertStatus> statuses() {
        return Arbitraries.of(AlertStatus.values());
    }

    @Provide
    Arbitrary<List<Integer>> minuteOffsets() {
        return Arbitraries.integers()
                .between(0, 525_600)
                .list()
                .ofMinSize(2)
                .ofMaxSize(20);
    }

    // Feature: investments-opportunity-monitor, Property 7: Ordenacao do historico de alertas
    @Property(tries = 200)
    void alertHistory_returnsAlertsOrderedByCreatedAtDescending(
            @ForAll("minuteOffsets") List<Integer> offsets,
            @ForAll("statuses") AlertStatus status) {

        List<Alert> alerts = new ArrayList<>(buildAlertsFromOffsets(offsets, status));
        Collections.shuffle(alerts, new Random(offsets.hashCode()));

        List<Alert> sortedDescending = alerts.stream()
                .sorted(Comparator.comparing(Alert::getCreatedAt).reversed())
                .toList();

        AlertRepository repository = createStubRepository(sortedDescending);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        PageRequest pageRequest = new PageRequest(0, alerts.size());

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        List<AlertResponse> content = result.content();
        assertDescendingOrder(content);
    }

    // Feature: investments-opportunity-monitor, Property 7: Ordenacao do historico de alertas
    @Property(tries = 200)
    void alertHistory_maintainsDescendingOrderWithMixedStatuses(
            @ForAll("minuteOffsets") List<Integer> offsets) {

        List<Alert> alerts = new ArrayList<>();
        AlertStatus[] values = AlertStatus.values();

        for (int i = 0; i < offsets.size(); i++) {
            LocalDateTime createdAt = BASE_TIME.plusMinutes(offsets.get(i));
            AlertStatus alertStatus = values[i % values.length];
            LocalDateTime sentAt = alertStatus == AlertStatus.SENT ? createdAt.plusMinutes(5) : null;

            alerts.add(new Alert(
                    (long) (i + 1), USER_ID, (long) (i + 1), null,
                    "XPLG11", alertStatus, "Details " + i,
                    createdAt, sentAt
            ));
        }

        List<Alert> sortedDescending = alerts.stream()
                .sorted(Comparator.comparing(Alert::getCreatedAt).reversed())
                .toList();

        AlertRepository repository = createStubRepository(sortedDescending);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        PageRequest pageRequest = new PageRequest(0, alerts.size());

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        assertDescendingOrder(result.content());
    }

    // Feature: investments-opportunity-monitor, Property 7: Ordenacao do historico de alertas
    @Property(tries = 200)
    void alertHistory_singleAlertIsTriviallyOrdered(
            @ForAll("statuses") AlertStatus status,
            @ForAll @IntRange(min = 0, max = 525_600) int minuteOffset) {

        LocalDateTime createdAt = BASE_TIME.plusMinutes(minuteOffset);
        LocalDateTime sentAt = status == AlertStatus.SENT ? createdAt.plusMinutes(5) : null;

        Alert alert = new Alert(1L, USER_ID, 1L, null, "MXRF11", status, "Details", createdAt, sentAt);

        AlertRepository repository = createStubRepository(List.of(alert));
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        PageRequest pageRequest = new PageRequest(0, 10);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        assert result.content().size() == 1 :
                "Expected exactly 1 alert but got %d".formatted(result.content().size());
        assertDescendingOrder(result.content());
    }

    // Feature: investments-opportunity-monitor, Property 7: Ordenacao do historico de alertas
    @Property(tries = 200)
    void alertHistory_filteredByTickerMaintainsDescendingOrder(
            @ForAll("minuteOffsets") List<Integer> offsets,
            @ForAll("statuses") AlertStatus status) {

        String targetTicker = "HGLG11";
        List<Alert> alerts = new ArrayList<>();

        for (int i = 0; i < offsets.size(); i++) {
            LocalDateTime createdAt = BASE_TIME.plusMinutes(offsets.get(i));
            LocalDateTime sentAt = status == AlertStatus.SENT ? createdAt.plusMinutes(5) : null;

            alerts.add(new Alert(
                    (long) (i + 1), USER_ID, (long) (i + 1), null,
                    targetTicker, status, "Details " + i,
                    createdAt, sentAt
            ));
        }

        List<Alert> sortedDescending = alerts.stream()
                .sorted(Comparator.comparing(Alert::getCreatedAt).reversed())
                .toList();

        AlertRepository repository = createStubRepository(sortedDescending);
        ListAlertHistoryUseCaseImpl useCase = new ListAlertHistoryUseCaseImpl(repository);

        AlertFilterCommand filter = new AlertFilterCommand(targetTicker, null);
        PageRequest pageRequest = new PageRequest(0, alerts.size());

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, pageRequest);

        assertDescendingOrder(result.content());
    }

    private List<Alert> buildAlertsFromOffsets(List<Integer> offsets, AlertStatus status) {
        return IntStream.range(0, offsets.size())
                .mapToObj(i -> {
                    LocalDateTime createdAt = BASE_TIME.plusMinutes(offsets.get(i));
                    LocalDateTime sentAt = status == AlertStatus.SENT ? createdAt.plusMinutes(5) : null;
                    return new Alert(
                            (long) (i + 1), USER_ID, (long) (i + 1), null,
                            "XPLG11", status, "Details " + i,
                            createdAt, sentAt
                    );
                })
                .toList();
    }

    private void assertDescendingOrder(List<AlertResponse> content) {
        for (int i = 0; i < content.size() - 1; i++) {
            LocalDateTime current = content.get(i).createdAt();
            LocalDateTime next = content.get(i + 1).createdAt();
            assert !current.isBefore(next) :
                    "Alert history not in descending order at index %d: %s is before %s"
                            .formatted(i, current, next);
        }
    }

    private AlertRepository createStubRepository(List<Alert> sortedAlerts) {
        return new AlertRepository() {
            @Override
            public Alert save(Alert alert) { return alert; }

            @Override
            public PageResult<Alert> findByUserId(Long userId, PageRequest pageRequest) {
                List<Alert> filtered = sortedAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId))
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public PageResult<Alert> findByUserIdAndTicker(Long userId, String ticker, PageRequest pageRequest) {
                List<Alert> filtered = sortedAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId) && a.getTicker().equals(ticker))
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public PageResult<Alert> findByUserIdAndStatus(Long userId, AlertStatus status, PageRequest pageRequest) {
                List<Alert> filtered = sortedAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId) && a.getStatus() == status)
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public PageResult<Alert> findByUserIdTickerAndStatus(Long userId, String ticker, AlertStatus status, PageRequest pageRequest) {
                List<Alert> filtered = sortedAlerts.stream()
                        .filter(a -> a.getUserId().equals(userId) && a.getTicker().equals(ticker) && a.getStatus() == status)
                        .toList();
                return toPageResult(filtered, pageRequest);
            }

            @Override
            public List<Alert> findPending() {
                return sortedAlerts.stream().filter(a -> a.getStatus() == AlertStatus.PENDING).toList();
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
