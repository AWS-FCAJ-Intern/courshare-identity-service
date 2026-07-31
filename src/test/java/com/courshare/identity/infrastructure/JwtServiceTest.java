package com.courshare.identity.infrastructure;

import com.courshare.identity.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenExpiration(Duration.ofMinutes(15));
        properties.setRefreshTokenExpiration(Duration.ofDays(7));
        com.courshare.identity.config.RsaKeyProperties rsaProperties = new com.courshare.identity.config.RsaKeyProperties();
        jwtService = new JwtService(properties, rsaProperties);
    }

    @Test
    void generatesAndParsesAccessToken() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(
                "user-1", "test@example.com", List.of("STUDENT"));

        Claims claims = jwtService.parseToken(pair.accessToken());
        assertEquals("user-1", claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertTrue(jwtService.isAccessToken(claims));
    }

    @Test
    void generatesRefreshTokenWithDistinctType() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(
                "user-1", "test@example.com", List.of("STUDENT"));

        Claims accessClaims = jwtService.parseToken(pair.accessToken());
        Claims refreshClaims = jwtService.parseToken(pair.refreshToken());

        assertTrue(jwtService.isAccessToken(accessClaims));
        assertTrue(jwtService.isRefreshToken(refreshClaims));
    }
}
