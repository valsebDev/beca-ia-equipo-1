package com.ids.ms_tickets.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(ServiceEndpoint users, ServiceEndpoint catalog,
		Duration connectTimeout, Duration readTimeout) {

	public record ServiceEndpoint(String url) {
	}
}
