package com.ids.ms_tickets.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.ids.ms_tickets.model.Ticket;

/** Calcula las fechas límite de respuesta y resolución (RN de SLA, D10). */
@Component
public class SlaCalculator {

	public SlaDeadlines calculate(Instant createdAt, int responseHours, int resolutionHours) {
		return new SlaDeadlines(
				createdAt.plus(Duration.ofHours(responseHours)),
				createdAt.plus(Duration.ofHours(resolutionHours)));
	}

	public boolean isOverdue(Ticket ticket, Instant now) {
		return new SlaDeadlines(ticket.getResponseDueAt(), ticket.getResolutionDueAt()).isOverdue(ticket, now);
	}
}
