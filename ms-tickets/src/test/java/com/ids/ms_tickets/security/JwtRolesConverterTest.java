package com.ids.ms_tickets.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesConverterTest {

	private final JwtRolesConverter converter = new JwtRolesConverter();

	@Test
	void rolesClaim_isMappedToRolePrefixedAuthorities() {
		Jwt jwt = jwt(Map.of("roles", List.of("AGENTE")));

		assertThat(converter.convert(jwt))
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_AGENTE");
	}

	@Test
	void scopeClaim_doesNotGrantRoles() {
		Jwt jwt = jwt(Map.of("scope", "ADMIN SOLICITANTE"));

		assertThat(converter.convert(jwt)).isEmpty();
	}

	@Test
	void missingRolesClaim_yieldsNoAuthorities() {
		assertThat(converter.convert(jwt(Map.of()))).isEmpty();
	}

	private static Jwt jwt(Map<String, Object> extraClaims) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject("10")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900));
		extraClaims.forEach(builder::claim);
		return builder.build();
	}
}
