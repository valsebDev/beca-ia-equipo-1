package com.ids.ms_catalog.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

import static com.ids.ms_catalog.security.SecurityTestSupport.ADMIN_ROLE;
import static org.assertj.core.api.Assertions.assertThat;

class JwtRolesConverterTest {

	private static final String ROLES = "roles";

	private final Converter<Jwt, Collection<GrantedAuthority>> converter = new JwtRolesConverter();

	@Test
	void convierteElClaimRolesEnAutoridadesRoleEnMayusculas() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWith(ROLES, List.of("admin", "AGENTE")));

		assertThat(authorities).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ADMIN", "ROLE_AGENTE");
	}

	@Test
	void listaVaciaNoOtorgaRoles() {
		assertThat(converter.convert(jwtWith(ROLES, List.of()))).isEmpty();
	}

	@Test
	void claimAusenteNoOtorgaRoles() {
		assertThat(converter.convert(jwtWith("sub", "42"))).isEmpty();
	}

	@Test
	void claimConTipoInvalidoNoOtorgaRoles() {
		assertThat(converter.convert(jwtWith(ROLES, ADMIN_ROLE))).isEmpty();
	}

	@Test
	void ignoraElementosQueNoSonTextoOEnBlanco() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWith(ROLES, List.of(ADMIN_ROLE, 7, " ")));

		assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
	}

	@Test
	void scopeNoOtorgaRoles() {
		assertThat(converter.convert(jwtWith("scope", ADMIN_ROLE))).isEmpty();
	}

	private static Jwt jwtWith(String claim, Object value) {
		return Jwt.withTokenValue("token-de-prueba")
				.header("alg", "HS256")
				.claim(claim, value)
				.build();
	}
}
