package com.ids.ms_catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI catalogOpenApi() {
		return new OpenAPI().info(new Info()
				.title("ms-catalog — Catálogo de servicio")
				.version("v1")
				.description("Gestión de categorías, prioridades y políticas SLA de la mesa de ayuda."));
	}
}
