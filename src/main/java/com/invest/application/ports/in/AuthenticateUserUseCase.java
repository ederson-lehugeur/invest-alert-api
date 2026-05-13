package com.invest.application.ports.in;

import com.invest.application.commands.AuthenticateUserCommand;
import com.invest.application.responses.TokenResponse;

public interface AuthenticateUserUseCase {

    TokenResponse execute(AuthenticateUserCommand command);
}
