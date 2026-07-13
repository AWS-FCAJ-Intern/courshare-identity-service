package com.courshare.identity.application;

import com.courshare.identity.api.dto.ChangePasswordRequest;
import com.courshare.identity.api.dto.ProfileResponse;
import com.courshare.identity.api.dto.UpdateProfileRequest;
import com.courshare.identity.domain.User;
import com.courshare.identity.domain.UserRepository;
import com.courshare.identity.domain.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String userId) {
        User user = findUser(userId);
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return toResponse(user, roles);
    }

    @Transactional
    public ProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.fullName() != null) {
            user.setFullName(request.fullName().isBlank() ? null : request.fullName().trim());
        }
        userRepository.save(user);
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return toResponse(user, roles);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private ProfileResponse toResponse(User user, List<String> roles) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
