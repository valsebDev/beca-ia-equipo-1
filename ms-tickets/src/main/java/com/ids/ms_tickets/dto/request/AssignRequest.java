package com.ids.ms_tickets.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignRequest(@NotNull @Positive Long assigneeId) {
}
