package com.ids.ms_tickets.mapper;

import com.ids.ms_tickets.dto.response.HistoryResponse;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.model.Ticket;
import com.ids.ms_tickets.model.TicketHistory;

public final class TicketMapper {

	private TicketMapper() {
	}

	public static TicketResponse toResponse(Ticket ticket, boolean overdue) {
		return new TicketResponse(
				ticket.getId(),
				ticket.getTitle(),
				ticket.getDescription(),
				ticket.getStatus(),
				ticket.getCategoryId(),
				ticket.getPriorityId(),
				ticket.getPriorityCode(),
				ticket.getRequesterId(),
				ticket.getAssigneeId(),
				ticket.getResponseDueAt(),
				ticket.getResolutionDueAt(),
				overdue,
				ticket.getCreatedAt(),
				ticket.getUpdatedAt(),
				ticket.getResolvedAt(),
				ticket.getClosedAt());
	}

	public static HistoryResponse toHistoryResponse(TicketHistory history) {
		return new HistoryResponse(
				history.getEventType(),
				history.getFromStatus(),
				history.getToStatus(),
				history.getFromAssigneeId(),
				history.getToAssigneeId(),
				history.getChangedBy(),
				history.getComment(),
				history.getChangedAt());
	}
}
