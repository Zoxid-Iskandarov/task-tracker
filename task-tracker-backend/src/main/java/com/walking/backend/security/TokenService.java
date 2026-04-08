package com.walking.backend.security;

import com.walking.backend.domain.dto.auth.AuthResponse;
import com.walking.backend.domain.exception.AuthException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.Cookie;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    @Value("${security.jwt.redis.refresh_token_prefix}")
    private final String refreshTokenPrefix;

    @Value("${security.jwt.redis.user_token_prefix}")
    private final String userTokenPrefix;

    @Value("${security.jwt.refresh_token_expiration}")
    private final long refreshTokenExpiration;

    @Value("${security.jwt.cookie-name}")
    private String cookieName;

    public AuthResponse generateTokens(String username, Long userId, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);

        saveRefreshToken(refreshToken, userId);

        ResponseCookie refreshTokenCookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/auth/")
                .maxAge(Duration.ofMinutes(refreshTokenExpiration))
                .sameSite(Cookie.SameSite.LAX.attributeValue())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return new AuthResponse(accessToken);
    }

    public AuthResponse validateAndRefreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            throw new AuthException("Refresh token not passed");
        }

        String username = jwtService.extractUsername(refreshToken);

        String key = getTokenKey(refreshToken);
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new AuthException("Refresh token is revoked");
        }

        redisTemplate.delete(getUserTokenKey(userId));
        redisTemplate.delete(key);

        return generateTokens(username, Long.valueOf(userId), response);
    }

    public void deleteRefreshToken(Long userId) {
        String key = getUserTokenKey(userId);
        String token = redisTemplate.opsForValue().get(key);

        if (token != null) {
            redisTemplate.delete(getTokenKey(token));
            redisTemplate.delete(key);
        }
    }

    public void deleteRefreshToken(String refreshToken) {
        String key = getTokenKey(refreshToken);
        String userId = redisTemplate.opsForValue().get(key);

        if (userId != null) {
            redisTemplate.delete(getUserTokenKey(userId));
            redisTemplate.delete(key);
        }
    }

    private void saveRefreshToken(String token, Long userId) {
        redisTemplate.opsForValue()
                .set(getTokenKey(token), userId.toString(), refreshTokenExpiration, TimeUnit.MINUTES);

        redisTemplate.opsForValue()
                .set(getUserTokenKey(userId), token, refreshTokenExpiration, TimeUnit.MINUTES);
    }

    private String getTokenKey(String refreshToken) {
        return refreshTokenPrefix + refreshToken;
    }

    private String getUserTokenKey(Long userId) {
        return userTokenPrefix + userId;
    }

    private String getUserTokenKey(String userId) {
        return userTokenPrefix + userId;
    }
}
