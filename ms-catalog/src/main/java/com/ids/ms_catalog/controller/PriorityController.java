package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.dto.request.PriorityRequest;
import com.ids.ms_catalog.dto.request.SlaRequest;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.security.AdminGuard;
import com.ids.ms_catalog.service.PriorityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/priorities")
@RequiredArgsConstructor
public class PriorityController {

	private final PriorityService priorityService;
	private final AdminGuard adminGuard;

	@GetMapping
	public List<PriorityResponse> list() {
		return priorityService.findAll();
	}

	@GetMapping("/{id}")
	public PriorityResponse get(@PathVariable Long id) {
		return priorityService.findById(id);
	}

	@PostMapping
	public ResponseEntity<PriorityResponse> create(@Valid @RequestBody PriorityRequest request) {
		adminGuard.verifyActiveAdmin();
		PriorityResponse created = priorityService.create(request);
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
	}

	@PutMapping("/{id}/sla")
	public PriorityResponse updateSla(@PathVariable Long id, @Valid @RequestBody SlaRequest request) {
		adminGuard.verifyActiveAdmin();
		return priorityService.updateSla(id, request);
	}
}
