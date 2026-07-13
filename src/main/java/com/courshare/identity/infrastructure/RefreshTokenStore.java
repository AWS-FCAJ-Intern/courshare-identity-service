package com.courshare.identity.infrastructure;

import com.courshare.identity.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenStore {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public RefreshTokenStore(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    public void store(String jti, String userId) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + jti,
                userId,
                jwtProperties.getRefreshTokenExpiration()
        );
    }

    public boolean exists(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + jti));
    }

    public void revoke(String jti) {
        redisTemplate.delete(REFRESH_PREFIX + jti);
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "1",
                jwtProperties.getRefreshTokenExpiration()
        );
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
