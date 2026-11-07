package com.ids.ms_tickets.service;

import java.time.Instant;

import com.ids.ms_tickets.model.Ticket;
import com.ids.ms_tickets.model.TicketStatus;

/** Fechas límite calculadas al crear el ticket a partir del snapshot de SLA. */
public record SlaDeadlines(Instant responseDueAt, Instant resolutionDueAt) {

	public boolean isOverdue(Ticket ticket, Instant now) {
		TicketStatus status = ticket.getStatus();
		boolean closed = status == TicketStatus.RESUELTO || status == TicketStatus.CERRADO
				|| status == TicketStatus.CANCELADO;
		return !closed && now.isAfter(ticket.getResolutionDueAt());
	}
}
