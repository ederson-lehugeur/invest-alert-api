package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.SubscriptionPlan;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
public class User {

    @Setter private Long id;
    @Setter private String name;
    @Setter private String email;
    @Setter private String passwordHash;
    private LocalDateTime createdAt;
    @Setter private LocalDateTime updatedAt;
    @Setter private SubscriptionPlan subscriptionPlan;
    @Setter private boolean enabled;
    private Set<Role> roles;

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.subscriptionPlan = SubscriptionPlan.FREE;
        this.enabled = true;
        this.roles = new HashSet<>();
    }

    public User(Long id, String name, String email, String passwordHash,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.subscriptionPlan = SubscriptionPlan.FREE;
        this.enabled = true;
        this.roles = new HashSet<>();
    }

    public User(Long id, String name, String email, String passwordHash,
                LocalDateTime createdAt, LocalDateTime updatedAt,
                SubscriptionPlan subscriptionPlan, boolean enabled, Set<Role> roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.subscriptionPlan = subscriptionPlan != null ? subscriptionPlan : SubscriptionPlan.FREE;
        this.enabled = enabled;
        this.roles = roles != null ? roles : new HashSet<>();
    }
}
