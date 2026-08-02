package com.courshare.identity.application;

import com.courshare.identity.api.dto.AuthResponse;
import com.courshare.identity.api.dto.LoginRequest;
import com.courshare.identity.api.dto.RegisterRequest;
import com.courshare.identity.api.dto.SendOtpRequest;
import com.courshare.identity.api.dto.ValidateResponse;
import com.courshare.identity.config.JwtProperties;
import com.courshare.identity.domain.Role;
import com.courshare.identity.domain.RoleRepository;
import com.courshare.identity.domain.User;
import com.courshare.identity.domain.UserRepository;
import com.courshare.identity.domain.UserRole;
import com.courshare.identity.domain.UserRoleRepository;
import com.courshare.identity.infrastructure.EmailService;
import com.courshare.identity.infrastructure.JwtService;
import com.courshare.identity.infrastructure.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    public static final String DEFAULT_ROLE = "STUDENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore,
            JwtProperties jwtProperties,
            OtpService otpService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.jwtProperties = jwtProperties;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    public void sendOtp(SendOtpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        String otp = otpService.generateOtp();
        otpService.saveOtp(request.email(), otp);
        emailService.sendVerificationCode(request.email(), otp);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        if (!otpService.verifyOtp(request.email(), request.otp())) {
            throw new AuthException("Invalid or expired verification code");
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        userRepository.save(user);

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new NotFoundException("STUDENT role not found"));
        Role instructorRole = roleRepository.findByName("INSTRUCTOR")
                .orElseThrow(() -> new NotFoundException("INSTRUCTOR role not found"));

        userRoleRepository.save(new UserRole(user.getId(), studentRole.getId()));
        userRoleRepository.save(new UserRole(user.getId(), instructorRole.getId()));

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        Claims claims = parseRefreshToken(refreshToken);
        refreshTokenStore.revoke(claims.getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims = parseRefreshToken(refreshToken);
        String jti = claims.getId();

        if (refreshTokenStore.isBlacklisted(jti) || !refreshTokenStore.exists(jti)) {
            throw new AuthException("Refresh token is invalid or expired");
        }

        String userId = claims.getSubject();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        String newAccessToken = jwtService.generateAccessToken(userId, user.getEmail(), roles);

        return new AuthResponse(
                newAccessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration().toSeconds()
        );
    }

    @Transactional(readOnly = true)
    public ValidateResponse validate(String accessToken) {
        Claims claims = parseAccessToken(accessToken);
        List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
        return new ValidateResponse(
                claims.getSubject(),
                claims.get("email", String.class),
                roles
        );
    }

    private AuthResponse issueTokens(User user) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        JwtService.TokenPair tokens = jwtService.generateTokenPair(user.getId(), user.getEmail(), roles);
        refreshTokenStore.store(tokens.refreshJti(), user.getId());

        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                jwtProperties.getAccessTokenExpiration().toSeconds()
        );
    }

    private Claims parseAccessToken(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            if (!jwtService.isAccessToken(claims)) {
                throw new AuthException("Invalid access token");
            }
            return claims;
        } catch (JwtException ex) {
            throw new AuthException("Invalid access token");
        }
    }

    private Claims parseRefreshToken(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            if (!jwtService.isRefreshToken(claims)) {
                throw new AuthException("Invalid refresh token");
            }
            return claims;
        } catch (JwtException ex) {
            throw new AuthException("Invalid refresh token");
        }
    }
}
