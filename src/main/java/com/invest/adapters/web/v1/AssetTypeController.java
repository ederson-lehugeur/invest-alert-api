package com.invest.adapters.web.v1;

import com.invest.application.ports.in.GetSupportedIndicatorsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/asset-types")
@Tag(name = "Asset Types", description = "Operations for querying supported indicators per asset type")
@SecurityRequirement(name = "bearerAuth")
public class AssetTypeController {

    private final GetSupportedIndicatorsUseCase getSupportedIndicatorsUseCase;

    @GetMapping("/{assetType}/indicators")
    @Operation(
            summary = "Get supported indicators for an asset type",
            description = "Returns the list of indicator codes supported for the given asset type")
    @ApiResponse(responseCode = "200", description = "Indicators retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid asset type value")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<List<String>> getSupportedIndicators(
            @Parameter(description = "Asset type (FII, STOCK, CRYPTOCURRENCY)")
            @PathVariable String assetType) {
        List<String> indicators = getSupportedIndicatorsUseCase.execute(assetType);
        return ResponseEntity.ok(indicators);
    }
}
