package com.invest.adapters.persistence.entities;

import com.invest.domain.entities.enumerator.ComparisonOperator;
import com.invest.domain.entities.enumerator.RuleField;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker", nullable = false)
    private AssetEntity asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private RuleGroupEntity group;

    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false)
    private RuleField field;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false)
    private ComparisonOperator operator;

    @Column(name = "target_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
