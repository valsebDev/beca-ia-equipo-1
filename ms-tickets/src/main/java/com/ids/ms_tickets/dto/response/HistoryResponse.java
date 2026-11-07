package com.ids.ms_tickets.dto.response;

import java.time.Instant;

import com.ids.ms_tickets.model.HistoryEventType;
import com.ids.ms_tickets.model.TicketStatus;

public record HistoryResponse(
		HistoryEventType eventType,
		TicketStatus fromStatus,
		TicketStatus toStatus,
		Long fromAssigneeId,
		Long toAssigneeId,
		Long changedBy,
		String comment,
		Instant changedAt) {
}
