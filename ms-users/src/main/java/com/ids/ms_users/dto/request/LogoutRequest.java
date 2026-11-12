package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
    @NotBlank(message = "El refresh token es obligatorio")
    @Size(max = 128, message = "El refresh token no puede exceder 128 caracteres")
    String refreshToken
) {}
