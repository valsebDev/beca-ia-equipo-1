package com.ids.ms_catalog.mapper;

import com.ids.ms_catalog.dto.request.PriorityRequest;
import com.ids.ms_catalog.dto.request.SlaRequest;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.dto.response.SlaResponse;
import com.ids.ms_catalog.model.Priority;
import com.ids.ms_catalog.model.SlaPolicy;

public final class PriorityMapper {

	private PriorityMapper() {
	}

	public static Priority toEntity(PriorityRequest request) {
		Priority priority = new Priority();
		priority.setCode(request.code());
		priority.setName(request.name());
		priority.setLevel(request.level().shortValue());
		priority.setActive(true);
		SlaPolicy sla = new SlaPolicy();
		updateSla(sla, request.sla());
		sla.setPriority(priority);
		priority.setSla(sla);
		return priority;
	}

	public static void updateSla(SlaPolicy sla, SlaRequest request) {
		sla.setResponseHours(request.responseHours());
		sla.setResolutionHours(request.resolutionHours());
	}

	public static PriorityResponse toResponse(Priority priority) {
		return new PriorityResponse(priority.getId(), priority.getCode(), priority.getName(),
				priority.getLevel(), priority.isActive(), toSlaResponse(priority.getSla()));
	}

	public static SlaResponse toSlaResponse(SlaPolicy sla) {
		return new SlaResponse(sla.getResponseHours(), sla.getResolutionHours());
	}
}
