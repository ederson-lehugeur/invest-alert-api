package com.invest.application.ports.in;

import com.invest.application.commands.RegisterUserCommand;
import com.invest.application.responses.UserResponse;

public interface RegisterUserUseCase {

    UserResponse execute(RegisterUserCommand command);
}
