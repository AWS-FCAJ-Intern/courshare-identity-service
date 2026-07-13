package com.courshare.identity.infrastructure;

import com.courshare.identity.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair generateTokenPair(String userId, String email, List<String> roles) {
        String accessToken = buildToken(userId, email, roles, TYPE_ACCESS, jwtProperties.getAccessTokenExpiration());
        String refreshToken = buildToken(userId, email, roles, TYPE_REFRESH, jwtProperties.getRefreshTokenExpiration());
        String refreshJti = extractJti(refreshToken);
        return new TokenPair(accessToken, refreshToken, refreshJti);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    public String generateAccessToken(String userId, String email, List<String> roles) {
        return buildToken(userId, email, roles, TYPE_ACCESS, jwtProperties.getAccessTokenExpiration());
    }

    private String buildToken(String userId, String email, List<String> roles, String type, java.time.Duration expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey)
                .compact();
    }

    public record TokenPair(String accessToken, String refreshToken, String refreshJti) {
    }
}
