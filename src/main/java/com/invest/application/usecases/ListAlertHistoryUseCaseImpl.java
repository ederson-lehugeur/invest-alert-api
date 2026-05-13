package com.invest.application.usecases;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.application.responses.AlertResponse;
import com.invest.domain.entities.Alert;
import com.invest.domain.ports.in.ListAlertHistoryUseCase;
import com.invest.domain.ports.out.repositories.AlertRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListAlertHistoryUseCaseImpl implements ListAlertHistoryUseCase {

    private final AlertRepository alertRepository;

    @Override
    public PageResult<AlertResponse> execute(Long userId, AlertFilterCommand filter, PageRequest pageRequest) {
        log.info("M=execute, I=Listando historico de alertas, userId={}, ticker={}, status={}, page={}, size={}",
                userId, filter.ticker(), filter.status(), pageRequest.page(), pageRequest.size());

        PageResult<Alert> result = fetchAlerts(userId, filter, pageRequest);

        List<AlertResponse> responses = result.content().stream()
                .map(this::toResponse)
                .toList();

        log.info("M=execute, I=Historico de alertas listado com sucesso, userId={}, totalElements={}", userId, result.totalElements());
        return new PageResult<>(
                responses,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private PageResult<Alert> fetchAlerts(Long userId, AlertFilterCommand filter, PageRequest pageRequest) {
        if (filter.ticker() != null && filter.status() != null) {
            return alertRepository.findByUserIdTickerAndStatus(
                    userId, filter.ticker(), filter.status(), pageRequest);
        }
        if (filter.ticker() != null) {
            return alertRepository.findByUserIdAndTicker(userId, filter.ticker(), pageRequest);
        }
        if (filter.status() != null) {
            return alertRepository.findByUserIdAndStatus(userId, filter.status(), pageRequest);
        }
        return alertRepository.findByUserId(userId, pageRequest);
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTicker(),
                alert.getStatus(),
                alert.getDetails(),
                alert.getCreatedAt(),
                alert.getSentAt()
        );
    }
}
