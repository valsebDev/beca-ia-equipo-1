package com.ids.ms_tickets.dto.request;

import com.ids.ms_tickets.model.TicketStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(
		@NotNull TicketStatus status,
		@Size(max = 1000) String comment) {
}
