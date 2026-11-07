package com.ids.ms_tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ids.ms_tickets.client.CatalogGateway;
import com.ids.ms_tickets.client.PrioritySnapshot;
import com.ids.ms_tickets.client.UserGateway;
import com.ids.ms_tickets.dto.request.ChangeStatusRequest;
import com.ids.ms_tickets.dto.request.CreateTicketRequest;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.exception.BusinessRuleException;
import com.ids.ms_tickets.exception.ConflictException;
import com.ids.ms_tickets.exception.ForbiddenException;
import com.ids.ms_tickets.model.HistoryEventType;
import com.ids.ms_tickets.model.Ticket;
import com.ids.ms_tickets.model.TicketHistory;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.model.UserRole;
import com.ids.ms_tickets.repository.TicketHistoryRepository;
import com.ids.ms_tickets.repository.TicketRepository;
import com.ids.ms_tickets.security.CurrentUser;

class TicketServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
	private static final CurrentUser REQUESTER = new CurrentUser(10L, UserRole.SOLICITANTE);
	private static final CurrentUser OTHER_REQUESTER = new CurrentUser(11L, UserRole.SOLICITANTE);
	private static final CurrentUser ADMIN = new CurrentUser(1L, UserRole.ADMIN);
	private static final CurrentUser AGENT = new CurrentUser(5L, UserRole.AGENTE);

	private TicketRepository tickets;
	private TicketHistoryRepository histories;
	private CatalogGateway catalog;
	private UserGateway users;
	private TicketService service;

	@BeforeEach
	void setUp() {
		tickets = mock(TicketRepository.class);
		histories = mock(TicketHistoryRepository.class);
		catalog = mock(CatalogGateway.class);
		users = mock(UserGateway.class);
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		service = new TicketService(tickets, histories, catalog, users,
				new TicketStateMachine(), new SlaCalculator(), new TicketAccessPolicy(), clock);
	}

	@Test
	void createTicket_withValidData_calculatesSlaDeadlinesAndRecordsHistory() {
		when(catalog.getActivePriority(2L)).thenReturn(new PrioritySnapshot("ALTA", 4, 24));
		when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketResponse response = service.create(REQUESTER,
				new CreateTicketRequest("Impresora sin red", "La impresora del piso 2 no responde", 1L, 2L));

		assertThat(response.status()).isEqualTo(TicketStatus.ABIERTO);
		assertThat(response.priorityCode()).isEqualTo("ALTA");
		assertThat(response.responseDueAt()).isEqualTo(NOW.plusSeconds(4 * 3600));
		assertThat(response.resolutionDueAt()).isEqualTo(NOW.plusSeconds(24 * 3600));
		assertThat(response.overdue()).isFalse();

		ArgumentCaptor<TicketHistory> captor = ArgumentCaptor.forClass(TicketHistory.class);
		verify(histories).save(captor.capture());
		assertThat(captor.getValue().getEventType()).isEqualTo(HistoryEventType.CREATED);
	}

	@Test
	void createTicket_byAgent_throwsForbidden() {
		CreateTicketRequest request = new CreateTicketRequest("Impresora sin red", "La impresora no responde", 1L, 2L);

		assertThatThrownBy(() -> service.create(AGENT, request))
				.isInstanceOf(ForbiddenException.class);
		verify(tickets, org.mockito.Mockito.never()).save(any());
	}

	@Test
	void createTicket_withInactiveCategory_throwsBusinessRule() {
		org.mockito.Mockito.doThrow(new BusinessRuleException("CATEGORY_INVALID", "Categoría inactiva"))
				.when(catalog).ensureActiveCategory(99L);

		assertThatThrownBy(() -> service.create(REQUESTER,
				new CreateTicketRequest("Impresora sin red", "La impresora no responde", 99L, 2L)))
				.isInstanceOf(BusinessRuleException.class)
				.extracting("code").isEqualTo("CATEGORY_INVALID");
	}

	@Test
	void assignTicket_byAdminToActiveAgent_setsAssignedStatusAndRecordsHistory() {
		Ticket ticket = ticket(TicketStatus.ABIERTO, null);
		when(tickets.findById(100L)).thenReturn(Optional.of(ticket));
		when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketResponse response = service.assign(ADMIN, 100L, 5L);

		assertThat(response.status()).isEqualTo(TicketStatus.ASIGNADO);
		assertThat(response.assigneeId()).isEqualTo(5L);
		verify(users).ensureActiveAgent(5L);
		verify(histories).save(any(TicketHistory.class));
	}

	@Test
	void assignTicket_whenClosed_throwsConflict() {
		when(tickets.findById(100L)).thenReturn(Optional.of(ticket(TicketStatus.CERRADO, 5L)));

		assertThatThrownBy(() -> service.assign(ADMIN, 100L, 6L))
				.isInstanceOf(ConflictException.class)
				.extracting("code").isEqualTo("TICKET_INVALID_TRANSITION");
	}

	@Test
	void changeStatus_invalidTransition_throwsConflict() {
		when(tickets.findById(100L)).thenReturn(Optional.of(ticket(TicketStatus.ABIERTO, null)));

		assertThatThrownBy(() -> service.changeStatus(ADMIN, 100L,
				new ChangeStatusRequest(TicketStatus.RESUELTO, null)))
				.isInstanceOf(ConflictException.class)
				.hasMessage("No se puede pasar de ABIERTO a RESUELTO");
	}

	@Test
	void changeStatus_assigneeStartsWork_setsFirstResponseTimestamp() {
		Ticket ticket = ticket(TicketStatus.ASIGNADO, 5L);
		when(tickets.findById(100L)).thenReturn(Optional.of(ticket));
		when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketResponse response = service.changeStatus(AGENT, 100L,
				new ChangeStatusRequest(TicketStatus.EN_PROGRESO, "Iniciando diagnóstico"));

		assertThat(response.status()).isEqualTo(TicketStatus.EN_PROGRESO);
		assertThat(ticket.getFirstResponseAt()).isEqualTo(NOW);
	}

	@Test
	void changeStatus_toClosedByAgent_throwsForbidden() {
		when(tickets.findById(100L)).thenReturn(Optional.of(ticket(TicketStatus.RESUELTO, 5L)));

		assertThatThrownBy(() -> service.changeStatus(AGENT, 100L,
				new ChangeStatusRequest(TicketStatus.CERRADO, null)))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void getTicket_byUnrelatedRequester_throwsForbidden() {
		when(tickets.findById(100L)).thenReturn(Optional.of(ticket(TicketStatus.ABIERTO, null)));

		assertThatThrownBy(() -> service.get(OTHER_REQUESTER, 100L))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void listTickets_asRequester_returnsOnlyOwnTickets() {
		when(tickets.findByRequesterIdOrderByCreatedAtDesc(10L))
				.thenReturn(List.of(ticket(TicketStatus.ABIERTO, null)));

		List<TicketResponse> result = service.list(REQUESTER, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).requesterId()).isEqualTo(10L);
	}

	private Ticket ticket(TicketStatus status, Long assigneeId) {
		Ticket ticket = new Ticket();
		ticket.setId(100L);
		ticket.setTitle("Impresora sin red");
		ticket.setDescription("La impresora no responde");
		ticket.setStatus(status);
		ticket.setCategoryId(1L);
		ticket.setPriorityId(2L);
		ticket.setPriorityCode("ALTA");
		ticket.setRequesterId(10L);
		ticket.setAssigneeId(assigneeId);
		ticket.setResponseDueAt(NOW.plusSeconds(4 * 3600));
		ticket.setResolutionDueAt(NOW.plusSeconds(24 * 3600));
		ticket.setCreatedAt(NOW);
		ticket.setUpdatedAt(NOW);
		return ticket;
	}
}
