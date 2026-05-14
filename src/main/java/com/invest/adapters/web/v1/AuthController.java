package com.invest.adapters.web.v1;

import com.invest.application.commands.AuthenticateUserCommand;
import com.invest.application.commands.RefreshTokenCommand;
import com.invest.application.commands.RegisterUserCommand;
import com.invest.application.commands.RevokeRefreshTokenCommand;
import com.invest.application.ports.in.AuthenticateUserUseCase;
import com.invest.application.ports.in.RefreshTokenUseCase;
import com.invest.application.ports.in.RegisterUserUseCase;
import com.invest.application.ports.in.RevokeRefreshTokenUseCase;
import com.invest.application.responses.TokenResponse;
import com.invest.application.responses.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration and login operations")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final RevokeRefreshTokenUseCase revokeRefreshTokenUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          RevokeRefreshTokenUseCase revokeRefreshTokenUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.revokeRefreshTokenUseCase = revokeRefreshTokenUseCase;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided credentials")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterUserCommand command) {
        UserResponse response = registerUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates a user and returns a short-lived access token and a refresh token")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<TokenResponse> login(@RequestBody AuthenticateUserCommand command) {
        TokenResponse response = authenticateUserUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token. The used refresh token is rotated.")
    @ApiResponse(responseCode = "200", description = "New access token issued")
    @ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenCommand command) {
        TokenResponse response = refreshTokenUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the provided refresh token, invalidating the session")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    @ApiResponse(responseCode = "401", description = "Refresh token is invalid or not found")
    public ResponseEntity<Void> logout(@RequestBody RevokeRefreshTokenCommand command) {
        revokeRefreshTokenUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
