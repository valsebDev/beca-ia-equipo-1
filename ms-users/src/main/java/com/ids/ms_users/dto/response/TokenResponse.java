package com.ids.ms_users.dto.response;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String refreshToken,
    long refreshExpiresIn
) {}
