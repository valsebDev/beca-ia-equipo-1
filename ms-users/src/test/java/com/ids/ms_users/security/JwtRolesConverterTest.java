package com.ids.ms_users.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesConverterTest {

    private final JwtRolesConverter converter = new JwtRolesConverter();

    @Test
    void rolesClaimIsMappedToRolePrefixedAuthorities() {
        Jwt jwt = jwtWithClaim("roles", List.of("ADMIN", "AGENTE"));

        assertThat(authorities(jwt)).containsExactly("ROLE_ADMIN", "ROLE_AGENTE");
    }

    @Test
    void tokenWithoutRolesClaimGivesNoAuthorities() {
        Jwt jwt = jwtWithClaim("sub", "1");

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private List<String> authorities(Jwt jwt) {
        return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt jwtWithClaim(String name, Object value) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim(name, value)
                .build();
    }
}
