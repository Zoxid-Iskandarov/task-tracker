package com.walking.backend.security.authentication;

import com.walking.backend.domain.exception.AuthException;
import com.walking.backend.props.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
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
    private final AppProperties appProperties;

    public String generateAccessToken(String username) {
        return generateToken(username, appProperties.getSecurity().getJwt().getAccessTokenExpiration());
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, appProperties.getSecurity().getJwt().getRefreshTokenExpiration());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
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
        } catch (ExpiredJwtException e) {
            throw new AuthException("Token expired");
        } catch (JwtException e) {
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
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(appProperties.getSecurity().getJwt().getSecret()));
    }
}
