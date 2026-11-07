package com.ids.ms_tickets.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ids.ms_tickets.exception.ConflictException;
import com.ids.ms_tickets.model.TicketStatus;

import static com.ids.ms_tickets.model.TicketStatus.ABIERTO;
import static com.ids.ms_tickets.model.TicketStatus.ASIGNADO;
import static com.ids.ms_tickets.model.TicketStatus.CANCELADO;
import static com.ids.ms_tickets.model.TicketStatus.CERRADO;
import static com.ids.ms_tickets.model.TicketStatus.EN_PROGRESO;
import static com.ids.ms_tickets.model.TicketStatus.RESUELTO;

/** Transiciones de estado del ticket (ARCHITECTURE.md D6). ASIGNADO solo se alcanza vía asignación. */
@Component
public class TicketStateMachine {

	private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
			ABIERTO, EnumSet.of(ASIGNADO, CANCELADO),
			ASIGNADO, EnumSet.of(EN_PROGRESO, CANCELADO),
			EN_PROGRESO, EnumSet.of(RESUELTO),
			RESUELTO, EnumSet.of(CERRADO),
			CERRADO, EnumSet.noneOf(TicketStatus.class),
			CANCELADO, EnumSet.noneOf(TicketStatus.class));

	public boolean canTransition(TicketStatus from, TicketStatus to) {
		return TRANSITIONS.get(from).contains(to);
	}

	public void assertTransition(TicketStatus from, TicketStatus to) {
		if (!canTransition(from, to)) {
			throw new ConflictException("TICKET_INVALID_TRANSITION",
					"No se puede pasar de " + from + " a " + to);
		}
	}
}
