package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAssetUseCaseImplTest {

    @Mock
    private AssetRepository assetRepository;

    private GetAssetUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAssetUseCaseImpl(assetRepository);
    }

    @Test
    void shouldReturnAsset_whenTickerExists() {
        var now = LocalDateTime.now();
        var asset = new Asset(1L, "HGLG11", "CGHG Logistica", new BigDecimal("170.50"),
                new BigDecimal("8.5"), new BigDecimal("1.05"), now);

        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(asset));

        AssetResponse response = useCase.execute("HGLG11");

        assertEquals("HGLG11", response.ticker());
        assertEquals("CGHG Logistica", response.name());
        assertEquals(new BigDecimal("170.50"), response.currentPrice());
        assertEquals(new BigDecimal("8.5"), response.dividendYield());
        assertEquals(new BigDecimal("1.05"), response.pVp());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void shouldThrowAssetNotFoundException_whenTickerDoesNotExist() {
        when(assetRepository.findByTicker("INVALID")).thenReturn(Optional.empty());

        AssetNotFoundException exception = assertThrows(
                AssetNotFoundException.class,
                () -> useCase.execute("INVALID")
        );

        assertTrue(exception.getMessage().contains("INVALID"));
        verify(assetRepository).findByTicker("INVALID");
    }
}
