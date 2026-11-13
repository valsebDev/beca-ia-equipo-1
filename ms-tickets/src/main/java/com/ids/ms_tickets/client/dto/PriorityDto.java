package com.ids.ms_tickets.client.dto;

public record PriorityDto(Long id, String code, String name, int level, boolean active, SlaDto sla) {

	public record SlaDto(int responseHours, int resolutionHours) {
	}
}
