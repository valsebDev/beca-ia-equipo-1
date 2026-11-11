package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre completo debe tener entre 2 y 150 caracteres")
    String fullName,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,72}$",
    message = "La contraseña debe tener entre 8 y 72 caracteres e incluir al menos una letra y un dígito"
    )
    String password
) {}
