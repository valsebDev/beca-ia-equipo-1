package com.ids.ms_tickets.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ids.ms_tickets.client.dto.CategoryDto;
import com.ids.ms_tickets.client.dto.PriorityDto;
import com.ids.ms_tickets.exception.BusinessRuleException;
import com.ids.ms_tickets.exception.DependencyUnavailableException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RemoteCatalogGateway implements CatalogGateway {

	private final CatalogClient catalogClient;

	@Override
	public void ensureActiveCategory(Long categoryId) {
		CategoryDto category;
		try {
			category = catalogClient.getCategory(categoryId);
		} catch (HttpClientErrorException.NotFound ex) {
			throw categoryInvalid(categoryId, ex);
		} catch (ResourceAccessException ex) {
			throw new DependencyUnavailableException("ms-catalog no responde", ex);
		}
		if (!category.active()) {
			throw categoryInvalid(categoryId);
		}
	}

	@Override
	public PrioritySnapshot getActivePriority(Long priorityId) {
		PriorityDto priority;
		try {
			priority = catalogClient.getPriority(priorityId);
		} catch (HttpClientErrorException.NotFound ex) {
			throw priorityInvalid(priorityId, ex);
		} catch (ResourceAccessException ex) {
			throw new DependencyUnavailableException("ms-catalog no responde", ex);
		}
		if (!priority.active()) {
			throw priorityInvalid(priorityId);
		}
		return new PrioritySnapshot(priority.code(),
				priority.sla().responseHours(), priority.sla().resolutionHours());
	}

	private static BusinessRuleException categoryInvalid(Long categoryId) {
		return categoryInvalid(categoryId, null);
	}

	private static BusinessRuleException categoryInvalid(Long categoryId, Throwable cause) {
		return new BusinessRuleException("CATEGORY_INVALID",
				"La categoría " + categoryId + " no existe o está inactiva", cause);
	}

	private static BusinessRuleException priorityInvalid(Long priorityId) {
		return priorityInvalid(priorityId, null);
	}

	private static BusinessRuleException priorityInvalid(Long priorityId, Throwable cause) {
		return new BusinessRuleException("PRIORITY_INVALID",
				"La prioridad " + priorityId + " no existe o está inactiva", cause);
	}
}
