package com.invest.application.ports.in;

import com.invest.application.commands.RevokeRefreshTokenCommand;

public interface RevokeRefreshTokenUseCase {

    void execute(RevokeRefreshTokenCommand command);
}
