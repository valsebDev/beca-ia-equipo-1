package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull(message = "El estado is_active no puede ser nulo")
    Boolean isActive
) {}
