package com.ids.ms_tickets.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ids.ms_tickets.client.CatalogGateway;
import com.ids.ms_tickets.client.PrioritySnapshot;
import com.ids.ms_tickets.client.UserGateway;
import com.ids.ms_tickets.dto.request.ChangeStatusRequest;
import com.ids.ms_tickets.dto.request.CreateTicketRequest;
import com.ids.ms_tickets.dto.response.HistoryResponse;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.exception.ConflictException;
import com.ids.ms_tickets.exception.NotFoundException;
import com.ids.ms_tickets.mapper.TicketMapper;
import com.ids.ms_tickets.model.Ticket;
import com.ids.ms_tickets.model.TicketHistory;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.model.UserRole;
import com.ids.ms_tickets.repository.TicketHistoryRepository;
import com.ids.ms_tickets.repository.TicketRepository;
import com.ids.ms_tickets.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final TicketRepository tickets;
	private final TicketHistoryRepository histories;
	private final CatalogGateway catalog;
	private final UserGateway users;
	private final TicketStateMachine stateMachine;
	private final SlaCalculator slaCalculator;
	private final TicketAccessPolicy access;
	private final Clock clock;

	@Transactional
	public TicketResponse create(CurrentUser actor, CreateTicketRequest request) {
		access.requireRole(actor, UserRole.SOLICITANTE);
		catalog.ensureActiveCategory(request.categoryId());
		PrioritySnapshot priority = catalog.getActivePriority(request.priorityId());

		Instant now = Instant.now(clock);
		SlaDeadlines deadlines = slaCalculator.calculate(now, priority.responseHours(), priority.resolutionHours());

		Ticket ticket = new Ticket();
		ticket.setTitle(request.title());
		ticket.setDescription(request.description());
		ticket.setStatus(TicketStatus.ABIERTO);
		ticket.setCategoryId(request.categoryId());
		ticket.setPriorityId(request.priorityId());
		ticket.setPriorityCode(priority.code());
		ticket.setSlaResponseHours(priority.responseHours());
		ticket.setSlaResolutionHours(priority.resolutionHours());
		ticket.setResponseDueAt(deadlines.responseDueAt());
		ticket.setResolutionDueAt(deadlines.resolutionDueAt());
		ticket.setRequesterId(actor.id());
		ticket.setCreatedAt(now);
		ticket.setUpdatedAt(now);

		ticket = tickets.save(ticket);
		recordHistory(ticket, HistoryChange.created(), actor.id(), now);
		return toResponse(ticket, now);
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> list(CurrentUser actor, TicketStatus status) {
		Instant now = Instant.now(clock);
		List<Ticket> found = switch (actor.role()) {
			case SOLICITANTE -> tickets.findByRequesterIdOrderByCreatedAtDesc(actor.id());
			case AGENTE -> tickets.findByAssigneeIdOrderByCreatedAtDesc(actor.id());
			case ADMIN -> tickets.findAllByOrderByCreatedAtDesc();
		};
		return found.stream()
				.filter(ticket -> status == null || ticket.getStatus() == status)
				.map(ticket -> toResponse(ticket, now))
				.toList();
	}

	@Transactional(readOnly = true)
	public TicketResponse get(CurrentUser actor, Long id) {
		Ticket ticket = load(id);
		access.requireView(actor, ticket);
		return toResponse(ticket, Instant.now(clock));
	}

	@Transactional
	public TicketResponse assign(CurrentUser actor, Long id, Long assigneeId) {
		access.requireRole(actor, UserRole.ADMIN);
		Ticket ticket = load(id);

		TicketStatus from = ticket.getStatus();
		if (from != TicketStatus.ABIERTO && from != TicketStatus.ASIGNADO) {
			throw new ConflictException("TICKET_INVALID_TRANSITION",
					"Solo se puede asignar un ticket ABIERTO o ASIGNADO; estado actual: " + from);
		}
		users.ensureActiveAgent(assigneeId);

		Instant now = Instant.now(clock);
		Long previousAssignee = ticket.getAssigneeId();
		ticket.setAssigneeId(assigneeId);
		ticket.setStatus(TicketStatus.ASIGNADO);
		ticket.setUpdatedAt(now);

		ticket = tickets.save(ticket);
		recordHistory(ticket, HistoryChange.assigned(from, previousAssignee, assigneeId), actor.id(), now);
		return toResponse(ticket, now);
	}

	@Transactional
	public TicketResponse changeStatus(CurrentUser actor, Long id, ChangeStatusRequest request) {
		Ticket ticket = load(id);
		access.requireView(actor, ticket);

		TicketStatus from = ticket.getStatus();
		TicketStatus to = request.status();
		stateMachine.assertTransition(from, to);
		access.requireTransition(actor, ticket, to);

		Instant now = Instant.now(clock);
		ticket.setStatus(to);
		ticket.setUpdatedAt(now);
		switch (to) {
			case EN_PROGRESO -> {
				if (ticket.getFirstResponseAt() == null) {
					ticket.setFirstResponseAt(now);
				}
			}
			case RESUELTO -> ticket.setResolvedAt(now);
			case CERRADO -> ticket.setClosedAt(now);
			default -> {
				// Sin marca de tiempo adicional.
			}
		}

		ticket = tickets.save(ticket);
		recordHistory(ticket, HistoryChange.statusChanged(from, to, request.comment()), actor.id(), now);
		return toResponse(ticket, now);
	}

	@Transactional(readOnly = true)
	public List<HistoryResponse> history(CurrentUser actor, Long id) {
		Ticket ticket = load(id);
		access.requireView(actor, ticket);
		return histories.findByTicketIdOrderByChangedAtAsc(ticket.getId()).stream()
				.map(TicketMapper::toHistoryResponse)
				.toList();
	}

	private Ticket load(Long id) {
		return tickets.findById(id)
				.orElseThrow(() -> new NotFoundException("TICKET_NOT_FOUND", "El ticket " + id + " no existe"));
	}

	private void recordHistory(Ticket ticket, HistoryChange change, Long changedBy, Instant now) {
		TicketHistory history = new TicketHistory();
		history.setTicket(ticket);
		history.setEventType(change.eventType());
		history.setFromStatus(change.fromStatus());
		history.setToStatus(change.toStatus());
		history.setFromAssigneeId(change.fromAssigneeId());
		history.setToAssigneeId(change.toAssigneeId());
		history.setChangedBy(changedBy);
		history.setComment(change.comment());
		history.setChangedAt(now);
		histories.save(history);
	}

	private TicketResponse toResponse(Ticket ticket, Instant now) {
		return TicketMapper.toResponse(ticket, slaCalculator.isOverdue(ticket, now));
	}
}
