package com.ids.ms_tickets.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ids.ms_tickets.dto.request.AssignRequest;
import com.ids.ms_tickets.dto.request.ChangeStatusRequest;
import com.ids.ms_tickets.dto.request.CreateTicketRequest;
import com.ids.ms_tickets.dto.response.HistoryResponse;
import com.ids.ms_tickets.dto.response.TicketResponse;
import com.ids.ms_tickets.model.TicketStatus;
import com.ids.ms_tickets.model.UserRole;
import com.ids.ms_tickets.security.CurrentUser;
import com.ids.ms_tickets.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Identidad simulada con los encabezados X-User-Id y X-User-Role hasta la Práctica 3 (JWT). */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

	private final TicketService ticketService;

	@PostMapping
	public ResponseEntity<TicketResponse> create(
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Role") UserRole role,
			@Valid @RequestBody CreateTicketRequest request) {
		TicketResponse created = ticketService.create(new CurrentUser(userId, role), request);
		return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
	}

	@GetMapping
	public List<TicketResponse> list(
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Role") UserRole role,
			@RequestParam(required = false) TicketStatus status) {
		return ticketService.list(new CurrentUser(userId, role), status);
	}

	@GetMapping("/{id}")
	public TicketResponse get(
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Role") UserRole role,
			@PathVariable Long id) {
		return ticketService.get(new CurrentUser(userId, role), id);
	}

	@PatchMapping("/{id}/assignment")
	public TicketResponse assign(
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Role") UserRole role,
			@PathVariable Long id,
			@Valid @RequestBody AssignRequest request) {
		return ticketService.assign(new CurrentUser(userId, role), id, request.assigneeId());
	}

	@PatchMapping("/{id}/status")
	public TicketResponse changeStatus(
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Role") UserRole role,
			@PathVariable Long id,
			@Valid @RequestBody ChangeStatusRequest request) {
		return ticketService.changeStatus(new CurrentUser(userId, role), id, request);
	}

	@GetMapping("/{id}/history")
	public List<HistoryResponse> history(
			@RequestHeader("X-User-Id") Long userId,
			@RequestHeader("X-User-Role") UserRole role,
			@PathVariable Long id) {
		return ticketService.history(new CurrentUser(userId, role), id);
	}
}
