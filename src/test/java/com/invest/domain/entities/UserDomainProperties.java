package com.invest.domain.entities;

import com.invest.domain.entities.enumerator.SubscriptionPlan;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Property 1: User roles set is always initialized.
 * For any User object constructed via any constructor overload that does not explicitly
 * provide a roles set, the roles field must be non-null and mutable.
 * Validates: Requirements 2.5
 */
class UserDomainProperties {

    @Property
    void threeArgConstructorAlwaysInitializesRoles(
            @ForAll @NotBlank String name,
            @ForAll @NotBlank String email,
            @ForAll @NotBlank String passwordHash) {

        User user = new User(name, email, passwordHash);

        assertNotNull(user.getRoles(), "roles must not be null for 3-arg constructor");
        assertDoesNotThrow(() -> user.getRoles().add(new Role(1L, "ROLE_TEST")),
                "roles set must be mutable for 3-arg constructor");
    }

    @Property
    void sixArgConstructorAlwaysInitializesRoles(
            @ForAll @NotBlank String name,
            @ForAll @NotBlank String email,
            @ForAll @NotBlank String passwordHash) {

        User user = new User(1L, name, email, passwordHash, LocalDateTime.now(), LocalDateTime.now());

        assertNotNull(user.getRoles(), "roles must not be null for 6-arg constructor");
        assertDoesNotThrow(() -> user.getRoles().add(new Role(1L, "ROLE_TEST")),
                "roles set must be mutable for 6-arg constructor");
    }

    @Property
    void fullConstructorWithNullRolesAlwaysInitializesRoles(
            @ForAll @NotBlank String name,
            @ForAll @NotBlank String email,
            @ForAll @NotBlank String passwordHash) {

        User user = new User(1L, name, email, passwordHash,
                LocalDateTime.now(), LocalDateTime.now(),
                SubscriptionPlan.FREE, true, null);

        assertNotNull(user.getRoles(), "roles must not be null when null is passed to full constructor");
        assertDoesNotThrow(() -> user.getRoles().add(new Role(1L, "ROLE_TEST")),
                "roles set must be mutable when null is passed to full constructor");
    }

    @Property
    void fullConstructorWithProvidedRolesPreservesSet(
            @ForAll @NotBlank String name,
            @ForAll @NotBlank String email,
            @ForAll @NotBlank String passwordHash) {

        var providedRoles = new HashSet<Role>();
        providedRoles.add(new Role(1L, "ROLE_USER"));

        User user = new User(1L, name, email, passwordHash,
                LocalDateTime.now(), LocalDateTime.now(),
                SubscriptionPlan.FREE, true, providedRoles);

        assertNotNull(user.getRoles(), "roles must not be null when a set is provided");
        assertDoesNotThrow(() -> user.getRoles().add(new Role(2L, "ROLE_ADMIN")),
                "roles set must be mutable when a set is provided");
    }
}
