package com.ids.ms_users.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull(message = "El estado is_active no puede ser nulo")
    Boolean isActive
) {}
