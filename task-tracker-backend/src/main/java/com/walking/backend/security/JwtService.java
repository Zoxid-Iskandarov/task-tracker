package com.walking.backend.security;

import com.walking.backend.domain.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final TokenService tokenService;

    @Value("${security.jwt.secret}")
    private final String secret;

    @Value("${security.jwt.access_token_expiration}")
    private final long accessTokenExpiration;

    @Value("${security.jwt.refresh_token_expiration}")
    private final long refreshTokenExpiration;

    public String generateAccessToken(String username) {
        return generateToken(username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, refreshTokenExpiration);
    }

    public boolean isValidAccessToken(String token, String username, Long userId) {
        if (!extractUsername(token).equals(username) || isTokenExpired(token)) {
            return false;
        }
        Long tokenUserId = tokenService.getUserIdByAccessToken(token);

        return tokenUserId != null && tokenUserId.equals(userId);
    }

    public boolean isValidRefreshToken(String token, String username, Long userId) {
        if (!extractUsername(token).equals(username) || isTokenExpired(token)) {
            return false;
        }
        Long tokenUserId = tokenService.getUserIdByRefreshToken(token);

        return tokenUserId != null && tokenUserId.equals(userId);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new AuthException("Invalid or malformed token");
        }
    }

    private String generateToken(String username, long expiryTime) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expiryTime, TimeUnit.MINUTES.toChronoUnit());

        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSignKey())
                .compact();
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
    }
}
