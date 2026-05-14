package com.invest.infrastructure.config.security;

import com.invest.domain.entities.Permission;
import com.invest.domain.entities.Role;
import com.invest.domain.entities.User;
import com.invest.domain.entities.enumerator.SubscriptionPlan;
import com.invest.domain.ports.out.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void shouldLoadUserByEmailAndReturnCorrectAuthorities() {
        var alertCreate = new Permission(1L, "ALERT_CREATE");
        var alertUpdate = new Permission(2L, "ALERT_UPDATE");
        var alertDelete = new Permission(3L, "ALERT_DELETE");
        var roleUser = new Role(1L, "ROLE_USER", Set.of(alertCreate, alertUpdate, alertDelete));

        var user = new User(1L, "John", "john@example.com", "hashed",
                LocalDateTime.now(), LocalDateTime.now(),
                SubscriptionPlan.FREE, true, Set.of(roleUser));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("john@example.com");

        assertNotNull(details);
        assertEquals("john@example.com", details.getUsername());
        assertTrue(details.isEnabled());

        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("ALERT_CREATE", "ALERT_UPDATE", "ALERT_DELETE"), authorities);
    }

    @Test
    void shouldThrowUsernameNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void shouldReturnDisabledUserDetails_whenUserIsDisabled() {
        var user = new User(1L, "Jane", "jane@example.com", "hashed",
                LocalDateTime.now(), LocalDateTime.now(),
                SubscriptionPlan.FREE, false, new HashSet<>());

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("jane@example.com");

        assertFalse(details.isEnabled(), "UserDetails must reflect disabled state");
    }

    @Test
    void shouldDeduplicatePermissionsAcrossMultipleRoles() {
        var sharedPermission = new Permission(1L, "ALERT_CREATE");
        var roleUser = new Role(1L, "ROLE_USER", Set.of(sharedPermission));
        var roleAdmin = new Role(2L, "ROLE_ADMIN", Set.of(sharedPermission,
                new Permission(2L, "USER_MANAGE")));

        var user = new User(1L, "Admin", "admin@example.com", "hashed",
                LocalDateTime.now(), LocalDateTime.now(),
                SubscriptionPlan.PRO, true, Set.of(roleUser, roleAdmin));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@example.com");

        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("ALERT_CREATE", "USER_MANAGE"), authorities,
                "Duplicate permissions across roles must be deduplicated");
    }
}
