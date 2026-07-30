package com.courshare.identity.api;

import com.courshare.identity.api.dto.ChangePasswordRequest;
import com.courshare.identity.api.dto.ProfileResponse;
import com.courshare.identity.api.dto.UpdateProfileRequest;
import com.courshare.identity.application.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping
    public ProfileResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(authentication.getName(), request);
    }

    @GetMapping("/public/{userId}")
    public com.courshare.identity.api.dto.PublicProfileResponse getPublicProfile(
            @org.springframework.web.bind.annotation.PathVariable String userId
    ) {
        return profileService.getPublicProfile(userId);
    }
}
