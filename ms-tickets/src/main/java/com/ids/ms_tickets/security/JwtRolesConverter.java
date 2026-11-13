package com.ids.ms_tickets.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Convierte el claim {@code roles} en autoridades {@code ROLE_*}. El claim {@code scope} no concede roles. */
public class JwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private static final String ROLES_CLAIM = "roles";

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		List<String> roles = Objects.requireNonNullElse(jwt.getClaimAsStringList(ROLES_CLAIM), List.of());
		return roles.stream()
				.<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.toList();
	}
}
