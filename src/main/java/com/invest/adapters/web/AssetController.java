package com.invest.adapters.web;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.ports.in.GetAssetUseCase;
import com.invest.domain.ports.in.ListAssetsUseCase;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Operations for listing and retrieving monitored assets")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final ListAssetsUseCase listAssetsUseCase;
    private final GetAssetUseCase getAssetUseCase;

    public AssetController(ListAssetsUseCase listAssetsUseCase,
                           GetAssetUseCase getAssetUseCase) {
        this.listAssetsUseCase = listAssetsUseCase;
        this.getAssetUseCase = getAssetUseCase;
    }

    @GetMapping
    @Operation(summary = "List assets", description = "Returns a paginated list of all monitored assets")
    @ApiResponse(responseCode = "200", description = "Assets retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<PageResult<AssetResponse>> list(
            @Parameter(description = "Page index (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        PageResult<AssetResponse> result = listAssetsUseCase.execute(new PageRequest(page, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{ticker}")
    @Operation(summary = "Get asset by ticker", description = "Returns a single asset identified by its ticker symbol")
    @ApiResponse(responseCode = "200", description = "Asset retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Asset not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    public ResponseEntity<AssetResponse> get(
            @Parameter(description = "Asset ticker symbol") @PathVariable String ticker) {
        AssetResponse response = getAssetUseCase.execute(ticker);
        return ResponseEntity.ok(response);
    }
}
