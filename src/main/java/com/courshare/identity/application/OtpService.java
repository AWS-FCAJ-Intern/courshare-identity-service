package com.courshare.identity.application;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final String OTP_PREFIX = "otp:register:";
    private static final long OTP_TTL_MINUTES = 5;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateOtp() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    public void saveOtp(String email, String code) {
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, OTP_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public boolean verifyOtp(String email, String code) {
        String key = OTP_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
