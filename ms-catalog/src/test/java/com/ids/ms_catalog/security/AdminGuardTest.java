package com.ids.ms_catalog.security;

import com.ids.ms_catalog.client.UsersClient;
import com.ids.ms_catalog.client.dto.UserClientResponse;
import com.ids.ms_catalog.exception.DependencyUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGuardTest {

	private static final String ADMIN_SUBJECT = "42";

	@Mock
	private UsersClient usersClient;

	private AdminGuard adminGuard;

	@BeforeEach
	void setUp() {
		adminGuard = new AdminGuard(usersClient);
		SecurityContextHolder.getContext().setAuthentication(jwtAuthentication());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void adminActivoPermiteLaOperacion() {
		when(usersClient.findById(ADMIN_SUBJECT)).thenReturn(new UserClientResponse(true));

		assertDoesNotThrow(adminGuard::verifyActiveAdmin);
		verify(usersClient).findById(ADMIN_SUBJECT);
	}

	@Test
	void adminDesactivadoDevuelveAccesoDenegado() {
		when(usersClient.findById(ADMIN_SUBJECT)).thenReturn(new UserClientResponse(false));

		assertThrows(AccessDeniedException.class, adminGuard::verifyActiveAdmin);
	}

	@Test
	void usuarioInexistenteEnUsersDevuelveAccesoDenegado() {
		when(usersClient.findById(anyString())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

		assertThrows(AccessDeniedException.class, adminGuard::verifyActiveAdmin);
	}

	@Test
	void error5xxDeUsersDevuelveDependenciaNoDisponible() {
		when(usersClient.findById(anyString()))
				.thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThrows(DependencyUnavailableException.class, adminGuard::verifyActiveAdmin);
	}

	@Test
	void rechazoDeUsersPor401DevuelveDependenciaNoDisponible() {
		when(usersClient.findById(anyString())).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

		assertThrows(DependencyUnavailableException.class, adminGuard::verifyActiveAdmin);
	}

	@Test
	void sinRespuestaDeUsersDevuelveDependenciaNoDisponible() {
		when(usersClient.findById(anyString())).thenThrow(new ResourceAccessException("timeout"));

		assertThrows(DependencyUnavailableException.class, adminGuard::verifyActiveAdmin);
	}

	@Test
	void sinAutenticacionJwtNoConsultaUsers() {
		SecurityContextHolder.clearContext();

		assertThrows(AccessDeniedException.class, adminGuard::verifyActiveAdmin);
		verifyNoInteractions(usersClient);
	}

	private static JwtAuthenticationToken jwtAuthentication() {
		Instant now = Instant.now();
		Jwt jwt = Jwt.withTokenValue("token-de-prueba")
				.header("alg", "HS256")
				.subject(ADMIN_SUBJECT)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.build();
		return new JwtAuthenticationToken(jwt);
	}
}
