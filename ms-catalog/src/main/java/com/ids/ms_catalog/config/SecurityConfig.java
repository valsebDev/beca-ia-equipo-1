package com.ids.ms_catalog.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.ids.ms_catalog.security.JwtRolesConverter;
import com.ids.ms_catalog.security.ProblemResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String CATEGORIES = "/api/v1/categories";
	private static final String PRIORITIES = "/api/v1/priorities";
	private static final String ADMIN = "ADMIN";
	private static final String UNAUTHORIZED_DETAIL = "Token ausente, inválido o expirado";
	private static final String FORBIDDEN_DETAIL = "No tiene permisos para realizar esta operación";

	@Bean
	@SuppressFBWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED",
			justification = "API stateless: el JWT viaja en Authorization, no en cookies, y no hay sesión")
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) {
		AuthenticationEntryPoint entryPoint = authenticationEntryPoint(objectMapper);
		AccessDeniedHandler accessDeniedHandler = accessDeniedHandler(objectMapper);
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, CATEGORIES, PRIORITIES).hasRole(ADMIN)
						.requestMatchers(HttpMethod.PUT, CATEGORIES + "/{id}", PRIORITIES + "/{id}/sla").hasRole(ADMIN)
						.requestMatchers(HttpMethod.DELETE, CATEGORIES + "/{id}").hasRole(ADMIN)
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(oauth -> oauth
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		try {
			return http.build();
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo construir la cadena de seguridad", ex);
		}
	}

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new JwtRolesConverter());
		return converter;
	}

	private static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
		return (request, response, authException) -> ProblemResponseWriter.write(objectMapper, request, response,
				HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "No autenticado", UNAUTHORIZED_DETAIL);
	}

	private static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
		return (request, response, deniedException) -> ProblemResponseWriter.write(objectMapper, request, response,
				HttpStatus.FORBIDDEN, "FORBIDDEN", "Acceso denegado", FORBIDDEN_DETAIL);
	}
}
