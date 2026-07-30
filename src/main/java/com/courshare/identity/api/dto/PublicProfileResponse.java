package com.courshare.identity.api.dto;

public record PublicProfileResponse(
        String id,
        String email,
        String fullName
) {
}
