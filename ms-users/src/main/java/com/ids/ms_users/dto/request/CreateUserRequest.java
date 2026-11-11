package com.ids.ms_users.dto.request;

import com.ids.ms_users.domain.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
    @Valid
    @NotNull(message = "Los datos del usuario son obligatorios")
    RegisterUserRequest user,

    @NotNull(message = "El rol es obligatorio")
    Role role
) {}
