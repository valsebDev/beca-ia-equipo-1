package com.ids.ms_tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import com.ids.ms_tickets.security.JwtRolesConverter;
import com.ids.ms_tickets.security.ProblemAccessDeniedHandler;
import com.ids.ms_tickets.security.ProblemAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			ProblemAuthenticationEntryPoint authenticationEntryPoint,
			ProblemAccessDeniedHandler accessDeniedHandler) {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/tickets").hasRole("SOLICITANTE")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/tickets/*/assignment").hasRole("ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.getOrBuild();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new JwtRolesConverter());
		return converter;
	}
}
