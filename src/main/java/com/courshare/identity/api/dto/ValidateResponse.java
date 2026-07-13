package com.courshare.identity.api.dto;

import java.util.List;

public record ValidateResponse(
        String userId,
        String email,
        List<String> roles
) {
}
