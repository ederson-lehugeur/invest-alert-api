package com.invest.adapters.web.v1;

import com.invest.application.commands.CreateRuleCommand;
import com.invest.application.commands.UpdateRuleCommand;
import com.invest.application.ports.in.CreateRuleUseCase;
import com.invest.application.ports.in.DeleteRuleUseCase;
import com.invest.application.ports.in.ListRulesUseCase;
import com.invest.application.ports.in.UpdateRuleUseCase;
import com.invest.application.responses.RuleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/rules")
@Tag(name = "Rules", description = "Operations for creating, listing, updating, and deleting monitoring rules")
@SecurityRequirement(name = "bearerAuth")
public class RuleController {

    private final CreateRuleUseCase createRuleUseCase;
    private final ListRulesUseCase listRulesUseCase;
    private final UpdateRuleUseCase updateRuleUseCase;
    private final DeleteRuleUseCase deleteRuleUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ALERT_CREATE')")
    @Operation(summary = "Create a rule", description = "Creates a new monitoring rule for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Rule created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid rule field or operator")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    @ApiResponse(responseCode = "404", description = "Asset not found for the given ticker")
    public ResponseEntity<RuleResponse> create(@RequestBody CreateRuleCommand command) {
        Long userId = getAuthenticatedUserId();
        RuleResponse response = createRuleUseCase.execute(userId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List rules", description = "Returns all monitoring rules for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rules retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<List<RuleResponse>> list() {
        Long userId = getAuthenticatedUserId();
        List<RuleResponse> response = listRulesUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERT_UPDATE')")
    @Operation(summary = "Update a rule", description = "Updates an existing monitoring rule owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rule updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid rule field or operator")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    @ApiResponse(responseCode = "403", description = "Access denied - rule belongs to another user")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    public ResponseEntity<RuleResponse> update(
            @Parameter(description = "Rule ID") @PathVariable Long id,
            @RequestBody UpdateRuleCommand command) {
        Long userId = getAuthenticatedUserId();
        RuleResponse response = updateRuleUseCase.execute(userId, id, command);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERT_DELETE')")
    @Operation(summary = "Delete a rule", description = "Deletes a monitoring rule owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Rule deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    @ApiResponse(responseCode = "403", description = "Access denied - rule belongs to another user")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Rule ID") @PathVariable Long id) {
        Long userId = getAuthenticatedUserId();
        deleteRuleUseCase.execute(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long getAuthenticatedUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
