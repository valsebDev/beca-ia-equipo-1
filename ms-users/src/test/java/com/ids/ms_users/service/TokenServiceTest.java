package com.ids.ms_users.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class TokenServiceTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String ISSUER = "ms-users";
    private static final String AUDIENCE = "mesa-ayuda";
    private static final String USER_IDENTIFIER = "c2bc24d7-923d-4383-9425-c3b1573dd005";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), 15, ISSUER, AUDIENCE);
    }

    @Test
    void generateToken_firmaClaimsRequeridosYPermiteExtraerIdentificador() {
        User user = new User(USER_IDENTIFIER, "Ana", "García", "López", "ana@example.com",
                "encoded-password", Role.ADMIN, true, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", 42L);

        String token = tokenService.generateToken(user);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(ISSUER, claims.getIssuer());
        assertEquals(java.util.Set.of(AUDIENCE), claims.getAudience());
        assertEquals("42", claims.getSubject());
        assertEquals(USER_IDENTIFIER, claims.get("userIdentifier", String.class));
        assertEquals("ana@example.com", claims.get("email", String.class));
        assertEquals("Ana García López", claims.get("name", String.class));
        assertEquals(List.of("ADMIN"), claims.get("roles"));
        assertNotNull(claims.getExpiration());
        assertEquals(USER_IDENTIFIER, tokenService.extractUserIdentifier("Bearer " + token));
        assertEquals(List.of("ADMIN"), tokenService.extractRoles("Bearer " + token));
    }

    @Test
    void extractUserIdentifier_tokenFirmadoAlterado_lanzaCredencialesInvalidas() {
        User user = new User(USER_IDENTIFIER, "Ana", "García", "López", "ana@example.com",
                "encoded-password", Role.ADMIN, true, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", 42L);
        String[] parts = tokenService.generateToken(user).split("\\.");
        String payload = parts[1];
        parts[1] = (payload.charAt(0) == 'A' ? "B" : "A") + payload.substring(1);

        assertThrows(InvalidCredentialsException.class,
                () -> tokenService.extractUserIdentifier("Bearer " + String.join(".", parts)));
    }

    @Test
    void extractUserIdentifier_sinBearerOTokenInvalido_lanzaCredencialesInvalidas() {
        assertThrows(InvalidCredentialsException.class, () -> tokenService.extractUserIdentifier(null));
        assertThrows(InvalidCredentialsException.class, () -> tokenService.extractUserIdentifier("Basic token"));
        assertThrows(InvalidCredentialsException.class, () -> tokenService.extractUserIdentifier("Bearer invalid"));
    }

    @Test
    void extractRoles_sinClaimDeRoles_devuelveListaVacia() {
        Date now = new Date();
        String tokenWithoutRoles = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject("42")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertEquals(List.of(), tokenService.extractRoles("Bearer " + tokenWithoutRoles));
    }
}
