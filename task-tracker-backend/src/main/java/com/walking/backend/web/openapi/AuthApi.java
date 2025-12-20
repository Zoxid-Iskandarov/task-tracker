package com.walking.backend.web.openapi;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.dto.auth.SignInRequest;
import com.walking.backend.domain.dto.auth.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "Managing registration, login, and token renewal")
public interface AuthApi {

    @Operation(summary = "New user registration", description = "Creates an account and returns a pair of JWT tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Input validation error"),
            @ApiResponse(responseCode = "409", description = "A user with this email or username already exists")
    })
    ResponseEntity<AuthResponse> signUp(@RequestBody @Validated SignUpRequest signUpRequest);

    @Operation(summary = "User authorization", description = "Authorizes the user and returns a pair of JWT tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful login"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    ResponseEntity<AuthResponse> signIn(@RequestBody @Validated SignInRequest signInRequest);

    @Operation(
            summary = "Refresh access token",
            description = "Extracts the refresh token from the 'X-Refresh-Token' header manually from the request object.",
            parameters = {
                    @Parameter(
                            name = "X-Refresh-Token",
                            in = ParameterIn.HEADER,
                            description = "The refresh token string",
                            required = true,
                            example = "eyJhbGciOiJIUzI1..."
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    ResponseEntity<AuthResponse> refreshTokens(HttpServletRequest request);
}
