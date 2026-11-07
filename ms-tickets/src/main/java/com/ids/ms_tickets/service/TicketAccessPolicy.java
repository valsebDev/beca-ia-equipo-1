package com.ids.ms_tickets.service;

import org.springframework.stereotype.Component;

import com.ids.ms_tickets.exception.ForbiddenException;
import com.ids.ms_tickets.model.Ticket;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.model.UserRole;
import com.ids.ms_tickets.security.CurrentUser;

/** Reglas de rol (RBAC) y de propiedad (dueño/asignado) según la matriz §6.3. */
@Component
public class TicketAccessPolicy {

	public void requireRole(CurrentUser actor, UserRole... allowed) {
		for (UserRole role : allowed) {
			if (actor.role() == role) {
				return;
			}
		}
		throw new ForbiddenException("Su rol no permite esta operación");
	}

	public void requireView(CurrentUser actor, Ticket ticket) {
		if (!canView(actor, ticket)) {
			throw new ForbiddenException("No tiene acceso a este ticket");
		}
	}

	public boolean canView(CurrentUser actor, Ticket ticket) {
		return actor.role() == UserRole.ADMIN || isOwner(actor, ticket) || isAssignee(actor, ticket);
	}

	public void requireTransition(CurrentUser actor, Ticket ticket, TicketStatus target) {
		boolean allowed = switch (target) {
			case EN_PROGRESO, RESUELTO -> actor.role() == UserRole.ADMIN || isAssignee(actor, ticket);
			case CERRADO -> actor.role() == UserRole.ADMIN || isOwner(actor, ticket);
			// SUPUESTO: el AGENTE asignado puede cancelar, como indica el Gherkin de HU-04.
			case CANCELADO -> actor.role() == UserRole.ADMIN || isOwner(actor, ticket) || isAssignee(actor, ticket);
			default -> false;
		};
		if (!allowed) {
			throw new ForbiddenException("No tiene permiso para cambiar el estado a " + target);
		}
	}

	private boolean isOwner(CurrentUser actor, Ticket ticket) {
		return ticket.getRequesterId().equals(actor.id());
	}

	private boolean isAssignee(CurrentUser actor, Ticket ticket) {
		return ticket.getAssigneeId() != null && ticket.getAssigneeId().equals(actor.id());
	}
}
