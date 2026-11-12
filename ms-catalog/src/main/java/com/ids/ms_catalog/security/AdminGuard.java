package com.ids.ms_catalog.security;

import com.ids.ms_catalog.client.UsersClient;
import com.ids.ms_catalog.client.dto.UserClientResponse;
import com.ids.ms_catalog.exception.DependencyUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Comprueba (D13) que el ADMIN autenticado siga activo en ms-users antes de una mutación.
 */
@Component
public class AdminGuard {

	private static final Logger LOG = LoggerFactory.getLogger(AdminGuard.class);
	private static final String FORBIDDEN_DETAIL = "No tiene permisos para realizar esta operación";
	private static final String UNAVAILABLE_DETAIL = "No se pudo verificar el estado del administrador";

	private final UsersClient usersClient;

	public AdminGuard(UsersClient usersClient) {
		this.usersClient = usersClient;
	}

	public void verifyActiveAdmin() {
		String subject = currentSubject();
		UserClientResponse user = findUser(subject);
		if (user == null || !user.active()) {
			throw new AccessDeniedException(FORBIDDEN_DETAIL);
		}
	}

	private static String currentSubject() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwt) {
			return jwt.getToken().getSubject();
		}
		throw new AccessDeniedException(FORBIDDEN_DETAIL);
	}

	private UserClientResponse findUser(String subject) {
		try {
			return usersClient.findById(subject);
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				throw new AccessDeniedException(FORBIDDEN_DETAIL, ex);
			}
			throw unavailable(ex);
		} catch (RestClientException ex) {
			throw unavailable(ex);
		}
	}

	private static DependencyUnavailableException unavailable(RestClientException ex) {
		LOG.warn("ms-users no disponible al verificar el administrador: {}", ex.getClass().getSimpleName());
		return new DependencyUnavailableException(UNAVAILABLE_DETAIL);
	}
}
