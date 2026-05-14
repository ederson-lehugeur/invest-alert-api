package com.invest.adapters.web.v1;

import com.invest.application.commands.CreateRuleGroupCommand;
import com.invest.application.ports.in.CreateRuleGroupUseCase;
import com.invest.application.ports.in.ListRuleGroupsUseCase;
import com.invest.application.responses.RuleGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rule-groups")
@Tag(name = "Rule Groups", description = "Operations for creating and listing rule groups")
@SecurityRequirement(name = "bearerAuth")
public class RuleGroupController {

    private final CreateRuleGroupUseCase createRuleGroupUseCase;
    private final ListRuleGroupsUseCase listRuleGroupsUseCase;

    public RuleGroupController(CreateRuleGroupUseCase createRuleGroupUseCase,
                               ListRuleGroupsUseCase listRuleGroupsUseCase) {
        this.createRuleGroupUseCase = createRuleGroupUseCase;
        this.listRuleGroupsUseCase = listRuleGroupsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ALERT_CREATE')")
    @Operation(summary = "Create a rule group", description = "Creates a new rule group with associated rules for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Rule group created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid rule field or operator in one of the group rules")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<RuleGroupResponse> create(@RequestBody CreateRuleGroupCommand command) {
        Long userId = getAuthenticatedUserId();
        RuleGroupResponse response = createRuleGroupUseCase.execute(userId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List rule groups", description = "Returns all rule groups for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rule groups retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<List<RuleGroupResponse>> list() {
        Long userId = getAuthenticatedUserId();
        List<RuleGroupResponse> response = listRuleGroupsUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }

    private Long getAuthenticatedUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
