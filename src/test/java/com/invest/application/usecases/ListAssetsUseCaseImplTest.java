package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.IndicatorValue;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.domain.ports.out.repositories.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAssetsUseCaseImplTest {

    @Mock
    private AssetRepository assetRepository;

    private ListAssetsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAssetsUseCaseImpl(assetRepository);
    }

    @Test
    void shouldReturnPaginatedAssets() {
        var now = LocalDateTime.now();
        var asset1 = fiiAsset(1L, "HGLG11", "CGHG Logistica", now);
        var asset2 = fiiAsset(2L, "XPML11", "XP Malls", now);

        var pageRequest = new PageRequest(0, 10);
        var domainPage = new PageResult<>(List.of(asset1, asset2), 0, 10, 2, 1);

        when(assetRepository.findAll(pageRequest)).thenReturn(domainPage);

        PageResult<AssetResponse> result = useCase.execute(pageRequest);

        assertEquals(2, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());

        AssetResponse first = result.content().get(0);
        assertEquals("HGLG11", first.ticker());
        assertEquals("CGHG Logistica", first.name());
        assertEquals("FII", first.assetType());
        assertEquals(3, first.indicators().size());
        assertEquals(now, first.updatedAt());
    }

    @Test
    void shouldReturnEmptyPage_whenNoAssetsExist() {
        var pageRequest = new PageRequest(0, 10);
        var emptyPage = new PageResult<Asset>(List.of(), 0, 10, 0, 0);

        when(assetRepository.findAll(pageRequest)).thenReturn(emptyPage);

        PageResult<AssetResponse> result = useCase.execute(pageRequest);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        var now = LocalDateTime.of(2025, 6, 15, 10, 30);
        var asset = fiiAsset(1L, "KNRI11", "Kinea Renda", now);

        var pageRequest = new PageRequest(0, 5);
        var domainPage = new PageResult<>(List.of(asset), 0, 5, 1, 1);

        when(assetRepository.findAll(pageRequest)).thenReturn(domainPage);

        PageResult<AssetResponse> result = useCase.execute(pageRequest);

        AssetResponse response = result.content().get(0);
        assertEquals("KNRI11", response.ticker());
        assertEquals("Kinea Renda", response.name());
        assertEquals("FII", response.assetType());
        assertEquals(3, response.indicators().size());
        assertEquals(now, response.updatedAt());
    }

    private Asset fiiAsset(Long id, String ticker, String name, LocalDateTime updatedAt) {
        return Asset.builder()
                .id(id)
                .ticker(ticker)
                .name(name)
                .assetType(AssetType.FII)
                .indicatorValues(List.of(
                        new IndicatorValue(IndicatorType.PRICE, new BigDecimal("170.50")),
                        new IndicatorValue(IndicatorType.DIVIDEND_YIELD, new BigDecimal("8.5")),
                        new IndicatorValue(IndicatorType.PVP, new BigDecimal("1.05"))
                ))
                .updatedAt(updatedAt)
                .build();
    }
}
