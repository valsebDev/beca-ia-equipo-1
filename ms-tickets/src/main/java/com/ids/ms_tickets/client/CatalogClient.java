package com.ids.ms_tickets.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.ids.ms_tickets.client.dto.CategoryDto;
import com.ids.ms_tickets.client.dto.PriorityDto;

@HttpExchange
public interface CatalogClient {

	@GetExchange("/api/v1/categories/{id}")
	CategoryDto getCategory(@PathVariable("id") Long id);

	@GetExchange("/api/v1/priorities/{id}")
	PriorityDto getPriority(@PathVariable("id") Long id);
}
