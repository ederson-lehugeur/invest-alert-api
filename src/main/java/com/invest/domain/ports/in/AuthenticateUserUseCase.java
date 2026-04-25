package com.invest.domain.ports.in;

import com.invest.application.commands.AuthenticateUserCommand;
import com.invest.application.responses.TokenResponse;

public interface AuthenticateUserUseCase {

    TokenResponse execute(AuthenticateUserCommand command);
}
