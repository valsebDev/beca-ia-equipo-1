package com.ids.ms_catalog.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Convierte solo el claim "roles" en autoridades ROLE_&lt;VALOR&gt;. Ignora "scope" y tipos inválidos.
 */
public class JwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private static final String ROLES_CLAIM = "roles";
	private static final String ROLE_PREFIX = "ROLE_";

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		if (jwt.getClaim(ROLES_CLAIM) instanceof List<?> roles) {
			for (Object role : roles) {
				if (role instanceof String value && !value.isBlank()) {
					authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + value.toUpperCase(Locale.ROOT)));
				}
			}
		}
		return authorities;
	}
}
