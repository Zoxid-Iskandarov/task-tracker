package com.walking.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final StringRedisTemplate redisTemplate;

    @Value("${security.jwt.redis.access_token_prefix}")
    private final String accessTokenPrefix;

    @Value("${security.jwt.redis.refresh_token_prefix}")
    private final String refreshTokenPrefix;

    @Value("${security.jwt.redis.user_token_prefix}")
    private final String userTokenPrefix;

    @Value("${security.jwt.access_token_expiration}")
    private final long accessTokenExpiration;

    @Value("${security.jwt.refresh_token_expiration}")
    private final long refreshTokenExpiration;

    public void saveAccessToken(String token, Long userId) {
        redisTemplate.opsForValue()
                .set(getAccessTokenKey(token), userId.toString(), accessTokenExpiration, TimeUnit.MINUTES);

        redisTemplate.opsForSet()
                .add(getUserTokenKey(userId), accessTokenPrefix + token);
    }

    public void saveRefreshToken(String token, Long userId) {
        redisTemplate.opsForValue()
                .set(getRefreshTokenKey(token), userId.toString(), refreshTokenExpiration, TimeUnit.MINUTES);

        redisTemplate.opsForSet()
                .add(getUserTokenKey(userId), refreshTokenPrefix + token);

        redisTemplate.expire(getUserTokenKey(userId), refreshTokenExpiration, TimeUnit.MINUTES);
    }

    public Long getUserIdByAccessToken(String token) {
        String value = redisTemplate.opsForValue()
                .get(getAccessTokenKey(token));

        return value != null ? Long.valueOf(value) : null;
    }

    public Long getUserIdByRefreshToken(String token) {
        String value = redisTemplate.opsForValue()
                .get(getRefreshTokenKey(token));

        return value != null ? Long.valueOf(value) : null;
    }

    public void deleteAllTokensOfUser(Long userId) {
        Set<String> tokens = redisTemplate.opsForSet()
                .members(getUserTokenKey(userId));

        for (String token : tokens) {
            if (token.startsWith(accessTokenPrefix)) {
                redisTemplate.delete(token);
            } else if (token.startsWith(refreshTokenPrefix)) {
                redisTemplate.delete(token);
            }
        }

        redisTemplate.delete(getUserTokenKey(userId));
    }

    private String getAccessTokenKey(String token) {
        return accessTokenPrefix + token;
    }

    private String getRefreshTokenKey(String token) {
        return refreshTokenPrefix + token;
    }

    private String getUserTokenKey(Long userId) {
        return userTokenPrefix + userId;
    }
}
