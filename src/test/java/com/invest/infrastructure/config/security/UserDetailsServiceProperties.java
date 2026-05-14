package com.invest.infrastructure.config.security;

import com.invest.domain.entities.Permission;
import com.invest.domain.entities.Role;
import com.invest.domain.entities.User;
import com.invest.domain.entities.enumerator.SubscriptionPlan;
import com.invest.domain.ports.out.repositories.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for UserDetailsServiceImpl.
 * Validates: Requirements 7.2, 7.4
 */
class UserDetailsServiceProperties {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);

    /**
     * Property 11: UserDetailsService resolves correct authorities.
     * For any user with any set of roles, where each role has any set of permissions,
     * loadUserByUsername must return a UserDetails whose getAuthorities() collection
     * contains exactly one GrantedAuthority per unique permission name across all roles.
     * Validates: Requirements 7.2
     */
    @Property(tries = 100)
    void userDetailsServiceResolvesCorrectAuthorities(
            @ForAll("usersWithRolesAndPermissions") User user) {

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(user.getEmail());

        assertNotNull(details, "UserDetails must not be null");

        Set<String> expectedAuthorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        Set<String> actualAuthorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(expectedAuthorities, actualAuthorities,
                "Authorities must match the unique permission names across all roles");
    }

    /**
     * Property 12: Disabled users produce non-enabled UserDetails.
     * For any user with enabled = false, loadUserByUsername must return a UserDetails
     * where isEnabled() returns false.
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    void disabledUsersProduceNonEnabledUserDetails(
            @ForAll @NotBlank String email) {

        User disabledUser = new User(1L, "Test User", email, "hash",
                LocalDateTime.now(), LocalDateTime.now(),
                SubscriptionPlan.FREE, false, new HashSet<>());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabledUser));

        UserDetails details = service.loadUserByUsername(email);

        assertFalse(details.isEnabled(),
                "UserDetails must have isEnabled() = false for a disabled user");
    }

    @Provide
    Arbitrary<User> usersWithRolesAndPermissions() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(local -> local + "@test.com")
                .flatMap(email -> {
                    Arbitrary<Set<Role>> rolesArbitrary = Arbitraries.integers()
                            .between(0, 3)
                            .flatMap(roleCount -> {
                                if (roleCount == 0) {
                                    return Arbitraries.just(new HashSet<>());
                                }
                                return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
                                        .map(String::toUpperCase)
                                        .list()
                                        .ofSize(roleCount)
                                        .map(roleNames -> {
                                            Set<Role> roles = new HashSet<>();
                                            for (int i = 0; i < roleNames.size(); i++) {
                                                Set<Permission> permissions = new HashSet<>();
                                                for (int j = 0; j < 3; j++) {
                                                    permissions.add(new Permission(
                                                            (long) (i * 10 + j),
                                                            "PERM_" + roleNames.get(i) + "_" + j
                                                    ));
                                                }
                                                roles.add(new Role((long) i, "ROLE_" + roleNames.get(i), permissions));
                                            }
                                            return roles;
                                        });
                            });

                    return rolesArbitrary.map(roles ->
                            new User(1L, "Test User", email, "hash",
                                    LocalDateTime.now(), LocalDateTime.now(),
                                    SubscriptionPlan.FREE, true, roles)
                    );
                });
    }
}
