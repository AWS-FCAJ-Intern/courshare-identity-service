package com.courshare.identity.api.dto;

import java.time.Instant;
import java.util.List;

public record ProfileResponse(
        String id,
        String email,
        String fullName,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
