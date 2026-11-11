package com.ids.ms_users.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds
) {}
