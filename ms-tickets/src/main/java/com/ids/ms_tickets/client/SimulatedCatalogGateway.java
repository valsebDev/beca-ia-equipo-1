package com.ids.ms_tickets.client;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ids.ms_tickets.exception.BusinessRuleException;

/** Simulación de ms-catalog hasta la Práctica 3; los valores coinciden con la semilla V2 del plan. */
@Component
public class SimulatedCatalogGateway implements CatalogGateway {

	private static final Set<Long> ACTIVE_CATEGORIES = Set.of(1L, 2L, 3L);

	private static final Map<Long, PrioritySnapshot> PRIORITIES = Map.of(
			1L, new PrioritySnapshot("CRITICA", 1, 4),
			2L, new PrioritySnapshot("ALTA", 4, 24),
			3L, new PrioritySnapshot("MEDIA", 8, 48),
			4L, new PrioritySnapshot("BAJA", 24, 120));

	@Override
	public void ensureActiveCategory(Long categoryId) {
		if (!ACTIVE_CATEGORIES.contains(categoryId)) {
			throw new BusinessRuleException("CATEGORY_INVALID",
					"La categoría " + categoryId + " no existe o está inactiva");
		}
	}

	@Override
	public PrioritySnapshot getActivePriority(Long priorityId) {
		PrioritySnapshot priority = PRIORITIES.get(priorityId);
		if (priority == null) {
			throw new BusinessRuleException("PRIORITY_INVALID",
					"La prioridad " + priorityId + " no existe o está inactiva");
		}
		return priority;
	}
}
