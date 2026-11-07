package com.ids.ms_tickets.dto.response;

import java.time.Instant;

import com.ids.ms_tickets.model.TicketStatus;

public record TicketResponse(
		Long id,
		String title,
		String description,
		TicketStatus status,
		Long categoryId,
		Long priorityId,
		String priorityCode,
		Long requesterId,
		Long assigneeId,
		Instant responseDueAt,
		Instant resolutionDueAt,
		boolean overdue,
		Instant createdAt,
		Instant updatedAt,
		Instant resolvedAt,
		Instant closedAt) {
}
