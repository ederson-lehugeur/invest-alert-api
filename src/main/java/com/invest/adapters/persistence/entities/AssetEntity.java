package com.invest.adapters.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, unique = true)
    private String ticker;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "dividend_yield", nullable = false, precision = 19, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "p_vp", nullable = false, precision = 19, scale = 4)
    private BigDecimal pVp;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
