package com.ids.ms_catalog.client;

import com.ids.ms_catalog.client.dto.UserClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface UsersClient {

	@GetExchange("/api/v1/users/{id}")
	UserClientResponse findById(@PathVariable("id") String id);
}
