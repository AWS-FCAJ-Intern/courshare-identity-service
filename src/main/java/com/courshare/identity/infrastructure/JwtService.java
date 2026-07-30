package com.courshare.identity.infrastructure;

import com.courshare.identity.config.JwtProperties;
import com.courshare.identity.config.RsaKeyProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final RsaKeyProperties rsaKeyProperties;

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtService(JwtProperties jwtProperties, RsaKeyProperties rsaKeyProperties) {
        this.jwtProperties = jwtProperties;
        this.rsaKeyProperties = rsaKeyProperties;

        RSAPrivateKey privKey = null;
        RSAPublicKey pubKey = null;

        try {
            // 1. Try loading from PEM String
            if (rsaKeyProperties.getPrivateKeyPem() != null && !rsaKeyProperties.getPrivateKeyPem().isBlank() &&
                rsaKeyProperties.getPublicKeyPem() != null && !rsaKeyProperties.getPublicKeyPem().isBlank()) {
                logger.info("Loading RSA key pair from PEM environment configuration...");
                privKey = RsaKeyHelper.parsePrivateKey(rsaKeyProperties.getPrivateKeyPem());
                pubKey = RsaKeyHelper.parsePublicKey(rsaKeyProperties.getPublicKeyPem());
            } 
            // 2. Try loading from Path
            else if (rsaKeyProperties.getPrivateKeyPath() != null && !rsaKeyProperties.getPrivateKeyPath().isBlank() &&
                     rsaKeyProperties.getPublicKeyPath() != null && !rsaKeyProperties.getPublicKeyPath().isBlank()) {
                logger.info("Loading RSA key pair from files: {} and {}", 
                        rsaKeyProperties.getPrivateKeyPath(), rsaKeyProperties.getPublicKeyPath());
                String privateKeyPem = RsaKeyHelper.readKeyFromFile(rsaKeyProperties.getPrivateKeyPath());
                String publicKeyPem = RsaKeyHelper.readKeyFromFile(rsaKeyProperties.getPublicKeyPath());
                privKey = RsaKeyHelper.parsePrivateKey(privateKeyPem);
                pubKey = RsaKeyHelper.parsePublicKey(publicKeyPem);
            }
        } catch (Exception e) {
            logger.error("Failed to load configured RSA keys, falling back to auto-generation", e);
        }

        // 3. Fallback: Auto-generate key pair for development
        if (privKey == null || pubKey == null) {
            KeyPair keyPair = RsaKeyHelper.generateRsaKeyPair();
            privKey = (RSAPrivateKey) keyPair.getPrivate();
            pubKey = (RSAPublicKey) keyPair.getPublic();
        }

        this.privateKey = privKey;
        this.publicKey = pubKey;
    }

    public RSAPublicKey getPublicKey() {
        return this.publicKey;
    }

    public String getKeyId() {
        return rsaKeyProperties.getKeyId();
    }

    public TokenPair generateTokenPair(String userId, String email, List<String> roles) {
        String accessToken = buildToken(userId, email, roles, TYPE_ACCESS, jwtProperties.getAccessTokenExpiration());
        String refreshToken = buildToken(userId, email, roles, TYPE_REFRESH, jwtProperties.getRefreshTokenExpiration());
        String refreshJti = extractJti(refreshToken);
        return new TokenPair(accessToken, refreshToken, refreshJti);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
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
                .header()
                    .keyId(rsaKeyProperties.getKeyId())
                    .and()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public record TokenPair(String accessToken, String refreshToken, String refreshJti) {
    }
}
