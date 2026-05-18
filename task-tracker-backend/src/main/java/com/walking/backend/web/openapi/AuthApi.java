package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and logout")
public interface AuthApi {

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account, generates access/refresh tokens, and sets the refresh token in a secure cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or user already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<AuthResponse> signUp(
            @RequestBody @Validated SignUpRequest signUpRequest,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(
            summary = "Authenticate user",
            description = "Verifies credentials, grants access/refresh tokens, and sets the refresh token in a secure cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    AuthResponse signIn(
            @RequestBody @Validated SignInRequest signInRequest,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(
            summary = "Refresh access token",
            description = "Uses the refresh token provided via cookies to issue a new pair of access and refresh tokens."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens successfully renewed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token is missing, expired, or invalid")
    })
    AuthResponse refreshTokens(
            @Parameter(in = ParameterIn.COOKIE, name = "refresh_token", description = "Secure HttpOnly refresh token")
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(
            summary = "Sign out user",
            description = "Logs out the user and clears authentication cookies. (Note: Intercepted and handled by Spring Security Logout Filter)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out")
    })
    ResponseEntity<Void> signOut();
}
