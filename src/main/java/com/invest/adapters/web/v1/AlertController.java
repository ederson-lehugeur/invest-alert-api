package com.invest.adapters.web.v1;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.application.responses.AlertResponse;
import com.invest.domain.entities.enumerator.AlertStatus;
import com.invest.domain.ports.in.ListAlertHistoryUseCase;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alerts")
@Tag(name = "Alerts", description = "Operations for viewing alert history")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final ListAlertHistoryUseCase listAlertHistoryUseCase;

    public AlertController(ListAlertHistoryUseCase listAlertHistoryUseCase) {
        this.listAlertHistoryUseCase = listAlertHistoryUseCase;
    }

    @GetMapping
    @Operation(summary = "List alert history", description = "Returns a paginated and optionally filtered list of alerts for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<PageResult<AlertResponse>> list(
            @Parameter(description = "Filter by asset ticker symbol") @RequestParam(required = false) String ticker,
            @Parameter(description = "Filter by alert status (PENDING or SENT)") @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "Page index (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Long userId = getAuthenticatedUserId();
        AlertFilterCommand filter = new AlertFilterCommand(ticker, status);
        PageResult<AlertResponse> result = listAlertHistoryUseCase.execute(
                userId, filter, new PageRequest(page, size));
        return ResponseEntity.ok(result);
    }

    private Long getAuthenticatedUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
