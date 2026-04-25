package com.invest.application.usecases;

import com.invest.application.commands.RegisterUserCommand;
import com.invest.application.responses.UserResponse;
import com.invest.domain.entities.User;
import com.invest.domain.exceptions.EmailAlreadyExistsException;
import com.invest.domain.ports.in.RegisterUserUseCase;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse execute(RegisterUserCommand command) {
        log.info("M=execute, I=Registrando usuario, email={}", command.email());

        if (userRepository.existsByEmail(command.email())) {
            log.warn("M=execute, W=Email ja cadastrado, email={}", command.email());
            throw new EmailAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        User user = new User(command.name(), command.email(), passwordHash);
        User savedUser = userRepository.save(user);

        log.info("M=execute, I=Usuario registrado com sucesso, userId={}", savedUser.getId());
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }
}
