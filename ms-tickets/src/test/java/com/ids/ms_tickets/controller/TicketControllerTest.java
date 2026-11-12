package com.ids.ms_tickets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ids.ms_tickets.config.JwtDecoderConfig;
import com.ids.ms_tickets.config.SecurityConfig;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.exception.ConflictException;
import com.ids.ms_tickets.exception.ForbiddenException;
import com.ids.ms_tickets.exception.NotFoundException;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.security.JwtRolesConverter;
import com.ids.ms_tickets.security.JwtTestTokens;
import com.ids.ms_tickets.security.ProblemAccessDeniedHandler;
import com.ids.ms_tickets.security.ProblemAuthenticationEntryPoint;
import com.ids.ms_tickets.service.TicketService;

@WebMvcTest(TicketController.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtDecoderConfig.class, JwtRolesConverter.class,
		ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
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
						.with(asRole("10", "SOLICITANTE"))
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
						.with(asRole("10", "SOLICITANTE"))
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
						.with(asRole("1", "ADMIN")))
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
						.with(asRole("1", "ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"RESUELTO\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TICKET_INVALID_TRANSITION"));
	}

	@Test
	void listTickets_returns200WithArray() throws Exception {
		when(ticketService.list(any(), any())).thenReturn(List.of(sampleResponse(1L)));

		mockMvc.perform(get("/api/v1/tickets")
						.with(asRole("10", "SOLICITANTE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void requestWithoutToken_returns401ProblemDetailWithBearerChallenge() throws Exception {
		mockMvc.perform(get("/api/v1/tickets"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("Bearer")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void requestWithBadSignature_returns401() throws Exception {
		String forged = JwtTestTokens.sign("otro-secreto-distinto-de-al-menos-32-bytes", claims -> { });

		mockMvc.perform(get("/api/v1/tickets")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void requestWithExpiredToken_returns401() throws Exception {
		String expired = JwtTestTokens.token(claims -> claims
				.issuedAt(java.time.Instant.now().minusSeconds(7200))
				.expiresAt(java.time.Instant.now().minusSeconds(3600)));

		mockMvc.perform(get("/api/v1/tickets")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void requestWithWrongIssuer_returns401() throws Exception {
		String token = JwtTestTokens.token(claims -> claims.issuer("otro-emisor"));

		mockMvc.perform(get("/api/v1/tickets")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void requestWithWrongAudience_returns401() throws Exception {
		String token = JwtTestTokens.token(claims -> claims.audience(List.of("otra-app")));

		mockMvc.perform(get("/api/v1/tickets")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createTicket_byAdmin_returns403() throws Exception {
		mockMvc.perform(post("/api/v1/tickets")
						.with(asRole("1", "ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void createTicket_byAgente_returns403() throws Exception {
		mockMvc.perform(post("/api/v1/tickets")
						.with(asRole("5", "AGENTE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isForbidden());
	}

	@Test
	void assignTicket_byNonAdmin_returns403() throws Exception {
		mockMvc.perform(patch("/api/v1/tickets/1/assignment")
						.with(asRole("5", "AGENTE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"assigneeId\":5}"))
				.andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@CsvSource({
			"GET,/api/v1/tickets",
			"GET,/api/v1/tickets/1",
			"GET,/api/v1/tickets/1/history",
			"POST,/api/v1/tickets",
			"PATCH,/api/v1/tickets/1/assignment",
			"PATCH,/api/v1/tickets/1/status"
	})
	void everyProtectedEndpoint_withoutToken_returns401(String method, String path) throws Exception {
		mockMvc.perform(request(HttpMethod.valueOf(method), path))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void getTicket_byOwner_returns200() throws Exception {
		when(ticketService.get(any(), anyLong())).thenReturn(sampleResponse(1L));

		mockMvc.perform(get("/api/v1/tickets/1")
						.with(asRole("10", "SOLICITANTE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void getTicket_byUnrelatedRequester_returns403() throws Exception {
		when(ticketService.get(any(), anyLong())).thenThrow(new ForbiddenException("No es dueño del ticket"));

		mockMvc.perform(get("/api/v1/tickets/1")
						.with(asRole("11", "SOLICITANTE")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void listTickets_withoutKnownRole_returns403() throws Exception {
		mockMvc.perform(get("/api/v1/tickets")
						.with(jwt().jwt(j -> j.subject("20").claim("roles", List.of("DESCONOCIDO")))))
				.andExpect(status().isForbidden());
	}

	@Test
	void getHistory_byUnrelatedRequester_returns403() throws Exception {
		when(ticketService.history(any(), anyLong())).thenThrow(new ForbiddenException("No es dueño del ticket"));

		mockMvc.perform(get("/api/v1/tickets/1/history")
						.with(asRole("11", "SOLICITANTE")))
				.andExpect(status().isForbidden());
	}

	@Test
	void assignTicket_byAdmin_returns200() throws Exception {
		when(ticketService.assign(any(), anyLong(), anyLong())).thenReturn(sampleResponse(1L));

		mockMvc.perform(patch("/api/v1/tickets/1/assignment")
						.with(asRole("1", "ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"assigneeId\":5}"))
				.andExpect(status().isOk());
	}

	@Test
	void changeStatus_byAssignedAgente_returns200() throws Exception {
		when(ticketService.changeStatus(any(), anyLong(), any())).thenReturn(sampleResponse(1L));

		mockMvc.perform(patch("/api/v1/tickets/1/status")
						.with(asRole("5", "AGENTE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"EN_PROGRESO\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void changeStatus_byUnassignedAgente_returns403() throws Exception {
		when(ticketService.changeStatus(any(), anyLong(), any())).thenThrow(
				new ForbiddenException("El agente no está asignado a este ticket"));

		mockMvc.perform(patch("/api/v1/tickets/1/status")
						.with(asRole("6", "AGENTE"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"EN_PROGRESO\"}"))
				.andExpect(status().isForbidden());
	}

	private static RequestPostProcessor asRole(String subject, String role) {
		JwtRequestPostProcessor jwtPost = jwt()
				.jwt(j -> j.subject(subject).claim("roles", List.of(role)))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role));
		return jwtPost;
	}

	private TicketResponse sampleResponse(Long id) {
		Instant now = Instant.parse("2026-11-05T10:00:00Z");
		return new TicketResponse(id, "Impresora sin red", "La impresora del piso 2 no responde",
				TicketStatus.ABIERTO, 1L, 2L, "ALTA", 10L, null,
				now.plusSeconds(4 * 3600), now.plusSeconds(24 * 3600), false,
				now, now, null, null);
	}
}
