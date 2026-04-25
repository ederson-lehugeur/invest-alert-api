package com.invest.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class User {

    @Setter private Long id;
    @Setter private String name;
    @Setter private String email;
    @Setter private String passwordHash;
    private LocalDateTime createdAt;
    @Setter private LocalDateTime updatedAt;

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
