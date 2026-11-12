package com.ids.ms_tickets.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import com.ids.ms_tickets.exception.ForbiddenException;
import com.ids.ms_tickets.model.UserRole;

class CurrentUserTest {

	@Test
	void from_readsSubjectAsIdAndRoleClaim() {
		CurrentUser user = CurrentUser.from(jwt("42", Map.of("roles", List.of("AGENTE"))));

		assertThat(user).isEqualTo(new CurrentUser(42L, UserRole.AGENTE));
	}

	@Test
	void from_nonNumericSubject_throwsBadCredentials() {
		assertThatThrownBy(() -> CurrentUser.from(jwt("ana", Map.of("roles", List.of("ADMIN")))))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void from_withoutKnownRole_throwsForbidden() {
		assertThatThrownBy(() -> CurrentUser.from(jwt("42", Map.of("roles", List.of("INVITADO")))))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void from_withoutRolesClaim_throwsForbidden() {
		assertThatThrownBy(() -> CurrentUser.from(jwt("42", Map.of())))
				.isInstanceOf(ForbiddenException.class);
	}

	private static Jwt jwt(String subject, Map<String, Object> extraClaims) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(subject)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900));
		extraClaims.forEach(builder::claim);
		return builder.build();
	}
}
