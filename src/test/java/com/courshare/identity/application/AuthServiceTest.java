package com.courshare.identity.application;

import com.courshare.identity.api.dto.LoginRequest;
import com.courshare.identity.api.dto.RegisterRequest;
import com.courshare.identity.config.JwtProperties;
import com.courshare.identity.domain.Role;
import com.courshare.identity.domain.RoleRepository;
import com.courshare.identity.domain.User;
import com.courshare.identity.domain.UserRepository;
import com.courshare.identity.domain.UserRole;
import com.courshare.identity.domain.UserRoleRepository;
import com.courshare.identity.infrastructure.JwtService;
import com.courshare.identity.infrastructure.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenStore refreshTokenStore;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-with-enough-length-for-hmac-sha256!!");
        properties.setAccessTokenExpiration(Duration.ofMinutes(15));
        properties.setRefreshTokenExpiration(Duration.ofDays(7));
        com.courshare.identity.config.RsaKeyProperties rsaProperties = new com.courshare.identity.config.RsaKeyProperties();
        JwtService jwtService = new JwtService(properties, rsaProperties);
        authService = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder,
                jwtService,
                refreshTokenStore,
                properties
        );
    }

    @Test
    void registerCreatesUserWithStudentAndInstructorRoles() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName("STUDENT"))
                .thenReturn(Optional.of(new Role("role-1", "STUDENT", "Learner")));
        when(roleRepository.findByName("INSTRUCTOR"))
                .thenReturn(Optional.of(new Role("role-2", "INSTRUCTOR", "Instructor")));
        when(userRoleRepository.findRoleNamesByUserId(anyString())).thenReturn(List.of("STUDENT", "INSTRUCTOR"));

        var response = authService.register(new RegisterRequest("new@example.com", "password123", "Test User", "STUDENT"));

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
    }

    @Test
    void loginFailsWithInvalidCredentials() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () ->
                authService.login(new LoginRequest("user@example.com", "wrong")));
    }

    @Test
    void registerFailsWhenEmailExists() {
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                authService.register(new RegisterRequest("exists@example.com", "password123", null, "STUDENT")));
    }
}
