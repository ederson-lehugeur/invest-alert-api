package com.invest.application.usecases;

import com.invest.application.commands.AuthenticateUserCommand;
import com.invest.application.responses.TokenResponse;
import com.invest.domain.entities.User;
import com.invest.domain.exceptions.InvalidCredentialsException;
import com.invest.domain.ports.in.AuthenticateUserUseCase;
import com.invest.domain.ports.out.PasswordEncoder;
import com.invest.domain.ports.out.TokenProvider;
import com.invest.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    public TokenResponse execute(AuthenticateUserCommand command) {
        log.info("M=execute, I=Iniciando autenticacao de usuario, email={}", command.email());

        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> {
                    log.warn("M=execute, W=Credenciais invalidas, email={}", command.email());
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            log.warn("M=execute, W=Senha incorreta, email={}", command.email());
            throw new InvalidCredentialsException();
        }

        String token = tokenProvider.generateToken(user);
        log.info("M=execute, I=Usuario autenticado com sucesso, userId={}", user.getId());
        return new TokenResponse(token, tokenProvider.getExpirationInSeconds());
    }
}
