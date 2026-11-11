package com.ids.ms_users.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull(message = "El estado active no puede ser nulo")
    Boolean active
) {}
