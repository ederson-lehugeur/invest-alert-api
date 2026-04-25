package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.ports.out.AssetRepository;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        var asset1 = new Asset(1L, "HGLG11", "CGHG Logistica", new BigDecimal("170.50"),
                new BigDecimal("8.5"), new BigDecimal("1.05"), now);
        var asset2 = new Asset(2L, "XPML11", "XP Malls", new BigDecimal("95.30"),
                new BigDecimal("7.2"), new BigDecimal("0.92"), now);

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
        assertEquals(new BigDecimal("170.50"), first.currentPrice());
        assertEquals(new BigDecimal("8.5"), first.dividendYield());
        assertEquals(new BigDecimal("1.05"), first.pVp());
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
        var asset = new Asset(1L, "KNRI11", "Kinea Renda", new BigDecimal("130.00"),
                new BigDecimal("6.8"), new BigDecimal("0.88"), now);

        var pageRequest = new PageRequest(0, 5);
        var domainPage = new PageResult<>(List.of(asset), 0, 5, 1, 1);

        when(assetRepository.findAll(pageRequest)).thenReturn(domainPage);

        PageResult<AssetResponse> result = useCase.execute(pageRequest);

        AssetResponse response = result.content().get(0);
        assertEquals("KNRI11", response.ticker());
        assertEquals("Kinea Renda", response.name());
        assertEquals(new BigDecimal("130.00"), response.currentPrice());
        assertEquals(new BigDecimal("6.8"), response.dividendYield());
        assertEquals(new BigDecimal("0.88"), response.pVp());
        assertEquals(now, response.updatedAt());
    }
}
