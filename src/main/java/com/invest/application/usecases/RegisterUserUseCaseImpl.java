package com.invest.application.usecases;

import com.invest.application.commands.RegisterUserCommand;
import com.invest.application.ports.in.RegisterUserUseCase;
import com.invest.application.responses.UserResponse;
import com.invest.domain.entities.Role;
import com.invest.domain.entities.User;
import com.invest.domain.entities.enumerator.SubscriptionPlan;
import com.invest.domain.exceptions.EmailAlreadyExistsException;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.repositories.RoleRepository;
import com.invest.domain.ports.out.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    public UserResponse execute(RegisterUserCommand command) {
        log.info("M=execute, I=Registrando usuario, email={}", command.email());

        if (userRepository.existsByEmail(command.email())) {
            log.warn("M=execute, W=Email ja cadastrado, email={}", command.email());
            throw new EmailAlreadyExistsException(command.email());
        }

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> {
                    log.error("M=execute, E=ROLE_USER not found in repository - system misconfiguration");
                    return new IllegalStateException("ROLE_USER not found. Ensure the DataInitializer has run.");
                });

        SubscriptionPlan plan = command.subscriptionPlan() != null
                ? command.subscriptionPlan()
                : SubscriptionPlan.FREE;

        String passwordHash = passwordEncoder.encode(command.password());
        User user = new User(command.name(), command.email(), passwordHash);
        user.setSubscriptionPlan(plan);
        user.setEnabled(true);
        user.getRoles().add(roleUser);

        User savedUser = userRepository.save(user);

        Set<String> roleNames = savedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        log.info("M=execute, I=Usuario registrado com sucesso, userId={}", savedUser.getId());
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getSubscriptionPlan(),
                roleNames,
                savedUser.getCreatedAt()
        );
    }
}
