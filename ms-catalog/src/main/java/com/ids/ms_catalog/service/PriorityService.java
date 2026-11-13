package com.ids.ms_catalog.service;

import com.ids.ms_catalog.config.properties.SlaProperties;
import com.ids.ms_catalog.dto.request.PriorityRequest;
import com.ids.ms_catalog.dto.request.SlaRequest;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.DomainValidationException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.mapper.PriorityMapper;
import com.ids.ms_catalog.model.Priority;
import com.ids.ms_catalog.repository.PriorityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriorityService {

	private final PriorityRepository priorityRepository;
	private final SlaProperties slaProperties;

	@Transactional
	public PriorityResponse create(PriorityRequest request) {
		validateSla(request.sla());
		if (priorityRepository.existsByCode(request.code())) {
			throw new ConflictException(
					"PRIORITY_ALREADY_EXISTS", "Ya existe una prioridad con código " + request.code());
		}
		if (priorityRepository.existsByLevel(request.level().shortValue())) {
			throw new ConflictException(
					"PRIORITY_LEVEL_ALREADY_EXISTS", "Ya existe una prioridad con nivel " + request.level());
		}
		return PriorityMapper.toResponse(priorityRepository.save(PriorityMapper.toEntity(request)));
	}

	@Transactional(readOnly = true)
	public List<PriorityResponse> findAll() {
		return priorityRepository.findAllWithSlaOrderByLevel().stream().map(PriorityMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public PriorityResponse findById(Long id) {
		return PriorityMapper.toResponse(getOrThrow(id));
	}

	@Transactional
	public PriorityResponse updateSla(Long id, SlaRequest request) {
		Priority priority = getOrThrow(id);
		validateSla(request);
		PriorityMapper.updateSla(priority.getSla(), request);
		return PriorityMapper.toResponse(priority);
	}

	private Priority getOrThrow(Long id) {
		return priorityRepository.findByIdWithSla(id)
				.orElseThrow(() -> new NotFoundException("PRIORITY_NOT_FOUND", "No existe una prioridad con id " + id));
	}

	private void validateSla(SlaRequest sla) {
		if (sla.responseHours() > slaProperties.maxResponseHours()) {
			throw new DomainValidationException("responseHours",
					"no puede superar " + slaProperties.maxResponseHours() + " horas");
		}
		if (sla.resolutionHours() > slaProperties.maxResolutionHours()) {
			throw new DomainValidationException("resolutionHours",
					"no puede superar " + slaProperties.maxResolutionHours() + " horas");
		}
		if (sla.resolutionHours() < sla.responseHours()) {
			throw new DomainValidationException("resolutionHours",
					"debe ser mayor o igual que responseHours");
		}
	}
}
