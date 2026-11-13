package com.ids.ms_tickets.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ids.ms_tickets.dto.request.AssignRequest;
import com.ids.ms_tickets.dto.request.ChangeStatusRequest;
import com.ids.ms_tickets.dto.request.CreateTicketRequest;
import com.ids.ms_tickets.dto.response.HistoryResponse;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.security.CurrentUser;
import com.ids.ms_tickets.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Endpoints de tickets. La identidad sale del JWT validado (sub y roles); la autorización está en SecurityConfig y en el service. */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

	private final TicketService ticketService;

	@PostMapping
	public ResponseEntity<TicketResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateTicketRequest request) {
		TicketResponse created = ticketService.create(CurrentUser.from(jwt), request);
		return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
	}

	@GetMapping
	public List<TicketResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) TicketStatus status) {
		return ticketService.list(CurrentUser.from(jwt), status);
	}

	@GetMapping("/{id}")
	public TicketResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ticketService.get(CurrentUser.from(jwt), id);
	}

	@PatchMapping("/{id}/assignment")
	public TicketResponse assign(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id,
			@Valid @RequestBody AssignRequest request) {
		return ticketService.assign(CurrentUser.from(jwt), id, request.assigneeId());
	}

	@PatchMapping("/{id}/status")
	public TicketResponse changeStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long id,
			@Valid @RequestBody ChangeStatusRequest request) {
		return ticketService.changeStatus(CurrentUser.from(jwt), id, request);
	}

	@GetMapping("/{id}/history")
	public List<HistoryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ticketService.history(CurrentUser.from(jwt), id);
	}
}
