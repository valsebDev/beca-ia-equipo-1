package com.ids.ms_tickets.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.ids.ms_tickets.client.dto.UserDto;

@HttpExchange
public interface UsersClient {

	@GetExchange("/api/v1/users/{id}")
	UserDto getUser(@PathVariable("id") Long id);
}
