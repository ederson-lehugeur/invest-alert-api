package com.invest.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
public class Role {

    private Long id;
    private String name;
    private Set<Permission> permissions;

    public Role(Long id, String name) {
        this.id = id;
        this.name = name;
        this.permissions = new HashSet<>();
    }
}
