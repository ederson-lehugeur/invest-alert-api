package com.invest.application.ports.in;

import com.invest.application.commands.RefreshTokenCommand;
import com.invest.application.responses.TokenResponse;

public interface RefreshTokenUseCase {

    TokenResponse execute(RefreshTokenCommand command);
}
