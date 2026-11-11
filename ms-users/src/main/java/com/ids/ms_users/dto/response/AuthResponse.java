package com.ids.ms_users.dto.response;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user
) {}
