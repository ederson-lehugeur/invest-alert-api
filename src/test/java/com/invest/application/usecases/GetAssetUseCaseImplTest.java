package com.invest.application.usecases;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.entities.Asset;
import com.invest.domain.entities.IndicatorValue;
import com.invest.domain.entities.enumerator.AssetType;
import com.invest.domain.entities.enumerator.IndicatorType;
import com.invest.domain.exceptions.AssetNotFoundException;
import com.invest.domain.ports.out.repositories.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        var asset = Asset.builder()
                .id(1L)
                .ticker("HGLG11")
                .name("CGHG Logistica")
                .assetType(AssetType.FII)
                .indicatorValues(List.of(
                        new IndicatorValue(IndicatorType.PRICE, new BigDecimal("170.50")),
                        new IndicatorValue(IndicatorType.DIVIDEND_YIELD, new BigDecimal("8.5")),
                        new IndicatorValue(IndicatorType.PVP, new BigDecimal("1.05"))
                ))
                .updatedAt(now)
                .build();

        when(assetRepository.findByTicker("HGLG11")).thenReturn(Optional.of(asset));

        AssetResponse response = useCase.execute("HGLG11");

        assertEquals("HGLG11", response.ticker());
        assertEquals("CGHG Logistica", response.name());
        assertEquals("FII", response.assetType());
        assertEquals(3, response.indicators().size());
        assertEquals(now, response.updatedAt());

        var priceIndicator = response.indicators().stream()
                .filter(iv -> "PRICE".equals(iv.code()))
                .findFirst();
        assertTrue(priceIndicator.isPresent());
        assertEquals(new BigDecimal("170.50"), priceIndicator.get().value());
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
