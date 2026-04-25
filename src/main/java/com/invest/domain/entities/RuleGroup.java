package com.invest.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class RuleGroup {

    @Setter private Long id;
    private Long userId;
    private String ticker;
    @Setter private String name;
    @Setter private List<Rule> rules;
    private LocalDateTime createdAt;

    public boolean evaluate(Asset asset) {
        return rules.stream().allMatch(rule -> rule.evaluate(asset));
    }
}
