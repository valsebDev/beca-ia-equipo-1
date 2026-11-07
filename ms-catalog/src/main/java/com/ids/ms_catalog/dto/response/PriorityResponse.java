package com.ids.ms_catalog.dto.response;

import com.ids.ms_catalog.model.PriorityCode;

public record PriorityResponse(Long id, PriorityCode code, String name, int level, boolean active,
		SlaResponse sla) {
}
