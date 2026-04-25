package com.invest.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class Alert {

    @Setter private Long id;
    private Long userId;
    private Long ruleId;
    private Long groupId;
    private String ticker;
    @Setter private AlertStatus status;
    @Setter private String details;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public void markAsSent() {
        this.status = AlertStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }
}
