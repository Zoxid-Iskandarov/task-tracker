package com.walking.backend.security.handler;

import com.walking.backend.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {
    private final TokenService tokenService;

    @Override
    public void logout(HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @Nullable Authentication authentication) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);
        Long userId = tokenService.getUserIdByAccessToken(token);

        if (userId != null) {
            tokenService.deleteAllTokensOfUser(userId);
        }
    }
}
