package com.ids.ms_tickets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import com.ids.ms_tickets.exception.ConflictException;
import com.ids.ms_tickets.exception.NotFoundException;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.service.TicketService;

@WebMvcTest(TicketController.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TicketControllerTest {

	private static final String VALID_BODY = """
			{"title":"Impresora sin red","description":"La impresora del piso 2 no responde",
			 "categoryId":1,"priorityId":2}
			""";

	private final MockMvc mockMvc;

	@MockitoBean
	private TicketService ticketService;

	TicketControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createTicket_withValidBody_returns201WithLocation() throws Exception {
		when(ticketService.create(any(), any())).thenReturn(sampleResponse(1L));

		mockMvc.perform(post("/api/v1/tickets")
						.header("X-User-Id", 10L)
						.header("X-User-Role", "SOLICITANTE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/tickets/1"))
				.andExpect(jsonPath("$.status").value("ABIERTO"));
	}

	@Test
	void createTicket_withBlankTitle_returns400ProblemDetailWithErrors() throws Exception {
		String body = """
				{"title":"","description":"La impresora del piso 2 no responde","categoryId":1,"priorityId":2}
				""";

		mockMvc.perform(post("/api/v1/tickets")
						.header("X-User-Id", 10L)
						.header("X-User-Role", "SOLICITANTE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("title"));
	}

	@Test
	void getTicket_whenMissing_returns404ProblemDetail() throws Exception {
		when(ticketService.get(any(), anyLong())).thenThrow(
				new NotFoundException("TICKET_NOT_FOUND", "El ticket 999 no existe"));

		mockMvc.perform(get("/api/v1/tickets/999")
						.header("X-User-Id", 1L)
						.header("X-User-Role", "ADMIN"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void changeStatus_invalidTransition_returns409ProblemDetail() throws Exception {
		when(ticketService.changeStatus(any(), anyLong(), any())).thenThrow(
				new ConflictException("TICKET_INVALID_TRANSITION", "No se puede pasar de ABIERTO a RESUELTO"));

		mockMvc.perform(patch("/api/v1/tickets/1/status")
						.header("X-User-Id", 1L)
						.header("X-User-Role", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"RESUELTO\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TICKET_INVALID_TRANSITION"));
	}

	@Test
	void listTickets_returns200WithArray() throws Exception {
		when(ticketService.list(any(), any())).thenReturn(List.of(sampleResponse(1L)));

		mockMvc.perform(get("/api/v1/tickets")
						.header("X-User-Id", 10L)
						.header("X-User-Role", "SOLICITANTE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void requestWithoutRoleHeader_returns400MalformedRequest() throws Exception {
		mockMvc.perform(get("/api/v1/tickets")
						.header("X-User-Id", 10L))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	private TicketResponse sampleResponse(Long id) {
		Instant now = Instant.parse("2026-11-05T10:00:00Z");
		return new TicketResponse(id, "Impresora sin red", "La impresora del piso 2 no responde",
				TicketStatus.ABIERTO, 1L, 2L, "ALTA", 10L, null,
				now.plusSeconds(4 * 3600), now.plusSeconds(24 * 3600), false,
				now, now, null, null);
	}
}
