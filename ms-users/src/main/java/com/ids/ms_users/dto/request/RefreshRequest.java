package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "El refresh token es obligatorio")
    String refreshToken
) {}
