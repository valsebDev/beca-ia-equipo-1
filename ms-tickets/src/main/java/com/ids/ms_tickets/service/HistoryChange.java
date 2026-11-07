package com.ids.ms_tickets.service;

import com.ids.ms_tickets.model.HistoryEventType;
import com.ids.ms_tickets.model.TicketStatus;

/** Descripción de un cambio que se registra en el historial del ticket. */
public record HistoryChange(
		HistoryEventType eventType,
		TicketStatus fromStatus,
		TicketStatus toStatus,
		Long fromAssigneeId,
		Long toAssigneeId,
		String comment) {

	public static HistoryChange created() {
		return new HistoryChange(HistoryEventType.CREATED, null, TicketStatus.ABIERTO, null, null, null);
	}

	public static HistoryChange assigned(TicketStatus fromStatus, Long fromAssigneeId, Long toAssigneeId) {
		return new HistoryChange(HistoryEventType.ASSIGNED, fromStatus, TicketStatus.ASIGNADO,
				fromAssigneeId, toAssigneeId, null);
	}

	public static HistoryChange statusChanged(TicketStatus fromStatus, TicketStatus toStatus, String comment) {
		return new HistoryChange(HistoryEventType.STATUS_CHANGED, fromStatus, toStatus, null, null, comment);
	}
}
