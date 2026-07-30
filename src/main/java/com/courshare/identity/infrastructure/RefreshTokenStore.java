package com.courshare.identity.infrastructure;

import com.courshare.identity.config.JwtProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenStore {

    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();
    private final Map<String, Boolean> blacklistedTokens = new ConcurrentHashMap<>();
    private final JwtProperties jwtProperties;

    public RefreshTokenStore(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public void store(String jti, String userId) {
        refreshTokens.put(jti, userId);
    }

    public boolean exists(String jti) {
        return refreshTokens.containsKey(jti);
    }

    public void revoke(String jti) {
        refreshTokens.remove(jti);
        blacklistedTokens.put(jti, true);
    }

    public boolean isBlacklisted(String jti) {
        return blacklistedTokens.containsKey(jti);
    }
}
