package com.ids.ms_tickets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI msTicketsOpenApi() {
		return new OpenAPI().info(new Info()
				.title("ms-tickets")
				.version("v1")
				.description("Gestión de tickets de la Mesa de Ayuda"));
	}
}
