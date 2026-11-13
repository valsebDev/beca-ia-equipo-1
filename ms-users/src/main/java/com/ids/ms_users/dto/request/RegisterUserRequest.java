package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String name,

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 100, message = "El apellido paterno no puede exceder 100 caracteres")
    String paternalName,

    @Size(max = 100, message = "El apellido materno no puede exceder 100 caracteres")
    String maternalName,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
    regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,50}$",
    message = "La contraseña debe tener entre 8 y 50 caracteres, "
            + "incluir mayúscula, minúscula, número y carácter especial"
    )
    String password
) {}
