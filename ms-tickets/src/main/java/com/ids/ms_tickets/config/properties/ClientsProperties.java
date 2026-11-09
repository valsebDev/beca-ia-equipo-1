package com.ids.ms_tickets.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(ServiceEndpoint users, ServiceEndpoint catalog) {

	public record ServiceEndpoint(String url) {
	}
}
