package com.ids.ms_tickets.security;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import com.ids.ms_tickets.exception.ForbiddenException;
import com.ids.ms_tickets.model.UserRole;

/** Identidad del llamador, tomada de los claims {@code sub} y {@code roles} del JWT validado. */
public record CurrentUser(Long id, UserRole role) {

	public static CurrentUser from(Jwt jwt) {
		return new CurrentUser(parseId(jwt.getSubject()), resolveRole(jwt.getClaimAsStringList("roles")));
	}

	private static Long parseId(String subject) {
		try {
			return Long.valueOf(subject);
		} catch (NumberFormatException ex) {
			throw new BadCredentialsException("El claim sub no es un identificador válido", ex);
		}
	}

	private static UserRole resolveRole(List<String> roles) {
		List<String> claimRoles = Objects.requireNonNullElse(roles, List.of());
		return Arrays.stream(UserRole.values())
				.filter(role -> claimRoles.contains(role.name()))
				.findFirst()
				.orElseThrow(() -> new ForbiddenException("El token no contiene un rol reconocido"));
	}
}
