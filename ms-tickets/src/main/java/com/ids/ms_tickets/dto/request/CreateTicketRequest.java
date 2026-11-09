package com.ids.ms_tickets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
		@NotBlank @Size(min = 5, max = 150) String title,
		@NotBlank @Size(min = 10, max = 4000) String description,
		@NotNull @Positive Long categoryId,
		@NotNull @Positive Long priorityId) {
}
