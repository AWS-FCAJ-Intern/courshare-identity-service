package com.courshare.identity.api.dto;

public record ErrorResponse(
        String message,
        int status
) {
}
