package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo no es válido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    String softPassword
) {}
