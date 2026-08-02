package com.courshare.identity.api;

import com.courshare.identity.api.dto.AuthResponse;
import com.courshare.identity.api.dto.LoginRequest;
import com.courshare.identity.api.dto.LogoutRequest;
import com.courshare.identity.api.dto.RefreshRequest;
import com.courshare.identity.api.dto.RegisterRequest;
import com.courshare.identity.api.dto.SendOtpRequest;
import com.courshare.identity.api.dto.ValidateResponse;
import com.courshare.identity.application.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/send-otp")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return Map.of("message", "Verification code sent successfully");
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/validate")
    public ValidateResponse validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String token = extractBearerToken(authorization);
        return authService.validate(token);
    }

    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new com.courshare.identity.application.AuthException("Missing or invalid Authorization header");
    }
}
