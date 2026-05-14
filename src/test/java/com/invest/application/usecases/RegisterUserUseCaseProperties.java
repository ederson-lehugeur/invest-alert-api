package com.invest.application.usecases;

import com.invest.application.commands.RegisterUserCommand;
import com.invest.application.responses.UserResponse;
import com.invest.domain.entities.Role;
import com.invest.domain.entities.User;
import com.invest.domain.entities.enumerator.SubscriptionPlan;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.repositories.RoleRepository;
import com.invest.domain.ports.out.repositories.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotBlank;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for RegisterUserUseCaseImpl.
 * Validates: Requirements 3.1, 3.3, 3.4, 3.5, 9.4
 */
class RegisterUserUseCaseProperties {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RegisterUserUseCaseImpl useCase =
            new RegisterUserUseCaseImpl(userRepository, passwordEncoder, roleRepository);

    private static final Role ROLE_USER = new Role(1L, "ROLE_USER");

    private void setupDefaultMocks(String email, String password) {
        reset(userRepository, passwordEncoder, roleRepository);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(ROLE_USER));
        when(passwordEncoder.encode(password)).thenReturn("hashed_" + password);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
    }

    /**
     * Property 2: Registration always assigns ROLE_USER.
     * For any valid RegisterUserCommand, when ROLE_USER exists in the repository,
     * the user saved to the repository must contain ROLE_USER in their roles set.
     * Validates: Requirements 3.1
     */
    @Property(tries = 100)
    void registrationAlwaysAssignsRoleUser(
            @ForAll @NotBlank String name,
            @ForAll("validEmails") String email,
            @ForAll @NotBlank String password) {

        setupDefaultMocks(email, password);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        useCase.execute(new RegisterUserCommand(name, email, password));

        User savedUser = captor.getValue();
        assertNotNull(savedUser.getRoles(), "Saved user roles must not be null");
        assertTrue(
                savedUser.getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getName())),
                "Saved user must contain ROLE_USER in their roles set"
        );
    }

    /**
     * Property 3: Registration defaults subscription plan to FREE.
     * For any RegisterUserCommand where subscriptionPlan is null, the saved user's
     * subscriptionPlan must equal SubscriptionPlan.FREE.
     * For any command where subscriptionPlan is explicitly set, the saved user's plan
     * must equal the provided value.
     * Validates: Requirements 3.3
     */
    @Property(tries = 100)
    void registrationDefaultsSubscriptionPlanToFreeWhenNull(
            @ForAll @NotBlank String name,
            @ForAll("validEmails") String email,
            @ForAll @NotBlank String password) {

        setupDefaultMocks(email, password);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // null subscriptionPlan must default to FREE
        useCase.execute(new RegisterUserCommand(name, email, password, null));

        assertEquals(SubscriptionPlan.FREE, captor.getValue().getSubscriptionPlan(),
                "Saved user must have FREE plan when subscriptionPlan is null");
    }

    @Property(tries = 100)
    void registrationPreservesExplicitSubscriptionPlan(
            @ForAll @NotBlank String name,
            @ForAll("validEmails") String email,
            @ForAll @NotBlank String password,
            @ForAll SubscriptionPlan plan) {

        setupDefaultMocks(email, password);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        useCase.execute(new RegisterUserCommand(name, email, password, plan));

        assertEquals(plan, captor.getValue().getSubscriptionPlan(),
                "Saved user must have the explicitly provided subscription plan");
    }

    /**
     * Property 4: Registered users are always enabled.
     * For any valid RegisterUserCommand, the user saved to the repository must have enabled = true.
     * Validates: Requirements 3.4
     */
    @Property(tries = 100)
    void registeredUsersAreAlwaysEnabled(
            @ForAll @NotBlank String name,
            @ForAll("validEmails") String email,
            @ForAll @NotBlank String password) {

        setupDefaultMocks(email, password);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        useCase.execute(new RegisterUserCommand(name, email, password));

        assertTrue(captor.getValue().isEnabled(),
                "Saved user must have enabled = true after registration");
    }

    /**
     * Property 5: UserResponse always includes roles after registration.
     * For any valid RegisterUserCommand, the UserResponse returned must contain
     * a non-empty roles set that includes "ROLE_USER".
     * Validates: Requirements 3.5
     */
    @Property(tries = 100)
    void userResponseAlwaysIncludesRolesAfterRegistration(
            @ForAll @NotBlank String name,
            @ForAll("validEmails") String email,
            @ForAll @NotBlank String password) {

        setupDefaultMocks(email, password);

        UserResponse response = useCase.execute(new RegisterUserCommand(name, email, password));

        assertNotNull(response.roles(), "UserResponse roles must not be null");
        assertTrue(response.roles().contains("ROLE_USER"),
                "UserResponse must include ROLE_USER in the roles set");
        assertTrue(response.roles().size() >= 1,
                "UserResponse roles set must be non-empty");
    }

    /**
     * Property 14: UserResponse always exposes subscription plan.
     * For any user with any SubscriptionPlan value, the UserResponse produced must
     * contain the same SubscriptionPlan value.
     * Validates: Requirements 9.4
     */
    @Property(tries = 100)
    void userResponseAlwaysExposesSubscriptionPlan(
            @ForAll @NotBlank String name,
            @ForAll("validEmails") String email,
            @ForAll @NotBlank String password,
            @ForAll SubscriptionPlan plan) {

        setupDefaultMocks(email, password);

        UserResponse response = useCase.execute(new RegisterUserCommand(name, email, password, plan));

        assertNotNull(response.subscriptionPlan(), "UserResponse subscriptionPlan must not be null");
        assertEquals(plan, response.subscriptionPlan(),
                "UserResponse must expose the same SubscriptionPlan value as provided in the command");
    }

    @Provide
    Arbitrary<String> validEmails() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(local -> local + "@test.com");
    }
}
