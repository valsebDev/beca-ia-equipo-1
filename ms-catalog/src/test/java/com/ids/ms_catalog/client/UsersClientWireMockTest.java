package com.ids.ms_catalog.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.ids.ms_catalog.client.dto.UserClientResponse;
import com.ids.ms_catalog.config.UsersClientConfig;
import com.ids.ms_catalog.config.properties.ClientsProperties;
import com.ids.ms_catalog.exception.DependencyUnavailableException;
import com.ids.ms_catalog.security.AdminGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsersClientWireMockTest {

	private static final String USER_ID = "42";
	private static final String PATH = "/api/v1/users/" + USER_ID;
	private static final String TOKEN = "token-de-prueba";
	private static final String BEARER = "Bearer " + TOKEN;
	private static final String ACTIVE_BODY = "{\"id\":42,\"email\":\"admin@example.test\","
			+ "\"fullName\":\"Admin User\",\"role\":\"ADMIN\",\"active\":true,"
			+ "\"createdAt\":\"2026-11-11T12:00:00Z\"}";
	private static final String INACTIVE_BODY = "{\"id\":42,\"email\":\"admin@example.test\","
			+ "\"fullName\":\"Admin User\",\"role\":\"ADMIN\",\"active\":false,"
			+ "\"createdAt\":\"2026-11-11T12:00:00Z\"}";
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration SHORT_READ_TIMEOUT = Duration.ofMillis(200);
	private static final int SLOW_RESPONSE_MILLIS = 1000;

	private WireMockServer server;
	private AdminGuard guard;

	@BeforeEach
	void setUp() {
		server = new WireMockServer(options().dynamicPort());
		server.start();
		guard = new AdminGuard(clientWith(READ_TIMEOUT));
		SecurityContextHolder.getContext().setAuthentication(jwtAuthentication());
	}

	@AfterEach
	void tearDown() {
		server.stop();
		SecurityContextHolder.clearContext();
	}

	@Test
	void respuesta200ConAdminActivoPermiteLaOperacion() {
		server.stubFor(get(urlEqualTo(PATH)).willReturn(okJson(ACTIVE_BODY)));

		assertDoesNotThrow(guard::verifyActiveAdmin);
		server.verify(getRequestedFor(urlEqualTo(PATH)).withHeader("Authorization", equalTo(BEARER)));
	}

	@Test
	void respuesta200MapeaLaRespuestaDefinidaEnElContrato() {
		server.stubFor(get(urlEqualTo(PATH)).willReturn(okJson(ACTIVE_BODY)));

		UserClientResponse user = clientWith(READ_TIMEOUT).findById(USER_ID);

		assertTrue(user.active());
	}

	@Test
	void respuesta200ConAdminInactivoDevuelveAccesoDenegado() {
		server.stubFor(get(urlEqualTo(PATH)).willReturn(okJson(INACTIVE_BODY)));

		assertThrows(AccessDeniedException.class, guard::verifyActiveAdmin);
	}

	@Test
	void respuesta404DevuelveAccesoDenegado() {
		server.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		assertThrows(AccessDeniedException.class, guard::verifyActiveAdmin);
	}

	@Test
	void respuesta401DelProveedorDevuelveDependenciaNoDisponible() {
		server.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));

		assertThrows(DependencyUnavailableException.class, guard::verifyActiveAdmin);
	}

	@Test
	void respuesta500DevuelveDependenciaNoDisponible() {
		server.stubFor(get(urlEqualTo(PATH))
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		assertThrows(DependencyUnavailableException.class, guard::verifyActiveAdmin);
	}

	@Test
	void conexionCerradaSinRespuestaDevuelveDependenciaNoDisponible() {
		server.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

		assertThrows(DependencyUnavailableException.class, guard::verifyActiveAdmin);
	}

	@Test
	void timeoutDevuelveDependenciaNoDisponible() {
		server.stubFor(get(urlEqualTo(PATH))
				.willReturn(okJson(ACTIVE_BODY).withFixedDelay(SLOW_RESPONSE_MILLIS)));
		AdminGuard slowGuard = new AdminGuard(clientWith(SHORT_READ_TIMEOUT));

		assertThrows(DependencyUnavailableException.class, slowGuard::verifyActiveAdmin);
	}

	private UsersClient clientWith(Duration readTimeout) {
		ClientsProperties.ServiceClient users =
				new ClientsProperties.ServiceClient(server.baseUrl(), CONNECT_TIMEOUT, readTimeout);
		return new UsersClientConfig().usersClient(new ClientsProperties(users));
	}

	private static JwtAuthenticationToken jwtAuthentication() {
		Instant now = Instant.now();
		Jwt jwt = Jwt.withTokenValue(TOKEN)
				.header("alg", "HS256")
				.subject(USER_ID)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.build();
		return new JwtAuthenticationToken(jwt);
	}
}
