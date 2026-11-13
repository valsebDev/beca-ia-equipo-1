package com.ids.ms_catalog.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración de los clientes HTTP hacia otros microservicios (clients.*).
 */
@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(ServiceClient users) {

	public record ServiceClient(String url, Duration connectTimeout, Duration readTimeout) {
	}
}
