package com.ids.ms_tickets.client;

public interface CatalogGateway {

	void ensureActiveCategory(Long categoryId);

	PrioritySnapshot getActivePriority(Long priorityId);
}
