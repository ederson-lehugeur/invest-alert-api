package com.invest.application.usecases;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.application.responses.AlertResponse;
import com.invest.domain.entities.Alert;
import com.invest.domain.entities.enumerator.AlertStatus;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListAlertHistoryUseCaseImplTest {

    @Mock
    private AlertRepository alertRepository;

    private ListAlertHistoryUseCaseImpl useCase;

    private static final Long USER_ID = 1L;
    private static final PageRequest PAGE_REQUEST = new PageRequest(0, 10);

    @BeforeEach
    void setUp() {
        useCase = new ListAlertHistoryUseCaseImpl(alertRepository);
    }

    @Test
    void shouldReturnPaginatedAlerts_whenNoFiltersApplied() {
        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        List<Alert> alerts = List.of(
                buildAlert(1L, USER_ID, "XPLG11", AlertStatus.SENT),
                buildAlert(2L, USER_ID, "HGLG11", AlertStatus.PENDING)
        );
        PageResult<Alert> pageResult = new PageResult<>(alerts, 0, 10, 2, 1);

        when(alertRepository.findByUserId(USER_ID, PAGE_REQUEST)).thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        assertEquals(2, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
        verify(alertRepository).findByUserId(USER_ID, PAGE_REQUEST);
        verifyNoMoreInteractions(alertRepository);
    }

    @Test
    void shouldFilterByTicker_whenTickerFilterProvided() {
        AlertFilterCommand filter = new AlertFilterCommand("XPLG11", null);
        List<Alert> alerts = List.of(
                buildAlert(1L, USER_ID, "XPLG11", AlertStatus.SENT)
        );
        PageResult<Alert> pageResult = new PageResult<>(alerts, 0, 10, 1, 1);

        when(alertRepository.findByUserIdAndTicker(USER_ID, "XPLG11", PAGE_REQUEST))
                .thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        assertEquals(1, result.content().size());
        assertEquals("XPLG11", result.content().getFirst().ticker());
        verify(alertRepository).findByUserIdAndTicker(USER_ID, "XPLG11", PAGE_REQUEST);
        verifyNoMoreInteractions(alertRepository);
    }

    @Test
    void shouldFilterByStatus_whenStatusFilterProvided() {
        AlertFilterCommand filter = new AlertFilterCommand(null, AlertStatus.PENDING);
        List<Alert> alerts = List.of(
                buildAlert(1L, USER_ID, "XPLG11", AlertStatus.PENDING)
        );
        PageResult<Alert> pageResult = new PageResult<>(alerts, 0, 10, 1, 1);

        when(alertRepository.findByUserIdAndStatus(USER_ID, AlertStatus.PENDING, PAGE_REQUEST))
                .thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        assertEquals(1, result.content().size());
        assertEquals(AlertStatus.PENDING, result.content().getFirst().status());
        verify(alertRepository).findByUserIdAndStatus(USER_ID, AlertStatus.PENDING, PAGE_REQUEST);
        verifyNoMoreInteractions(alertRepository);
    }

    @Test
    void shouldFilterByTickerAndStatus_whenBothFiltersProvided() {
        AlertFilterCommand filter = new AlertFilterCommand("HGLG11", AlertStatus.SENT);
        List<Alert> alerts = List.of(
                buildAlert(1L, USER_ID, "HGLG11", AlertStatus.SENT)
        );
        PageResult<Alert> pageResult = new PageResult<>(alerts, 0, 10, 1, 1);

        when(alertRepository.findByUserIdTickerAndStatus(USER_ID, "HGLG11", AlertStatus.SENT, PAGE_REQUEST))
                .thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        assertEquals(1, result.content().size());
        assertEquals("HGLG11", result.content().getFirst().ticker());
        assertEquals(AlertStatus.SENT, result.content().getFirst().status());
        verify(alertRepository).findByUserIdTickerAndStatus(USER_ID, "HGLG11", AlertStatus.SENT, PAGE_REQUEST);
        verifyNoMoreInteractions(alertRepository);
    }

    @Test
    void shouldReturnEmptyPage_whenUserHasNoAlerts() {
        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        PageResult<Alert> pageResult = new PageResult<>(List.of(), 0, 10, 0, 0);

        when(alertRepository.findByUserId(USER_ID, PAGE_REQUEST)).thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void shouldMapAlertFieldsCorrectly_toResponse() {
        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        LocalDateTime createdAt = LocalDateTime.of(2025, 6, 15, 10, 30);
        LocalDateTime sentAt = LocalDateTime.of(2025, 6, 15, 10, 35);
        Alert alert = new Alert(42L, USER_ID, 5L, null, "XPLG11",
                AlertStatus.SENT, "Preco abaixo de R$100", createdAt, sentAt);
        PageResult<Alert> pageResult = new PageResult<>(List.of(alert), 0, 10, 1, 1);

        when(alertRepository.findByUserId(USER_ID, PAGE_REQUEST)).thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        AlertResponse response = result.content().getFirst();
        assertEquals(42L, response.id());
        assertEquals("XPLG11", response.ticker());
        assertEquals(AlertStatus.SENT, response.status());
        assertEquals("Preco abaixo de R$100", response.details());
        assertEquals(createdAt, response.createdAt());
        assertEquals(sentAt, response.sentAt());
    }

    @Test
    void shouldQueryOnlyForRequestedUser_ensuringIsolation() {
        Long otherUserId = 99L;
        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        PageResult<Alert> pageResult = new PageResult<>(List.of(), 0, 10, 0, 0);

        when(alertRepository.findByUserId(otherUserId, PAGE_REQUEST)).thenReturn(pageResult);

        useCase.execute(otherUserId, filter, PAGE_REQUEST);

        verify(alertRepository).findByUserId(eq(otherUserId), eq(PAGE_REQUEST));
        verify(alertRepository, never()).findByUserId(eq(USER_ID), any());
    }

    @Test
    void shouldPreservePaginationMetadata_fromRepository() {
        AlertFilterCommand filter = new AlertFilterCommand(null, null);
        List<Alert> alerts = List.of(
                buildAlert(1L, USER_ID, "XPLG11", AlertStatus.PENDING)
        );
        PageResult<Alert> pageResult = new PageResult<>(alerts, 2, 5, 25, 5);

        when(alertRepository.findByUserId(USER_ID, PAGE_REQUEST)).thenReturn(pageResult);

        PageResult<AlertResponse> result = useCase.execute(USER_ID, filter, PAGE_REQUEST);

        assertEquals(2, result.page());
        assertEquals(5, result.size());
        assertEquals(25, result.totalElements());
        assertEquals(5, result.totalPages());
    }

    private Alert buildAlert(Long id, Long userId, String ticker, AlertStatus status) {
        return new Alert(id, userId, 1L, null, ticker, status,
                "Detalhes do alerta", LocalDateTime.now(), null);
    }
}
