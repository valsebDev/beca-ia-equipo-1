package com.ids.ms_users.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.ids.ms_users.config.JwtProperties;
import com.ids.ms_users.domain.User;

@Service
public class TokenService {

    private static final String CLAIM_ROLES = "roles";
    private static final long SECONDS_PER_MINUTE = 60L;

    private final JwtEncoder jwtEncoder;
    private final long accessTtlMinutes;
    private final String issuer;
    private final String audience;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.accessTtlMinutes = properties.accessTtlMinutes();
        this.issuer = properties.issuer();
        this.audience = properties.audience();
    }

    public String generateToken(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("El usuario debe tener un ID numérico antes de emitir el token");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(accessTtlMinutes, ChronoUnit.MINUTES))
                .claim(CLAIM_ROLES, List.of(user.getRole().name()))
                .claim("email", user.getEmail())
                .claim("name", fullName(user))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getAccessTtlSeconds() {
        return accessTtlMinutes * SECONDS_PER_MINUTE;
    }

    private static String fullName(User user) {
        String fullName = user.getName() + " " + user.getPaternalName();
        if (user.getMaternalName() != null && !user.getMaternalName().isBlank()) {
            fullName += " " + user.getMaternalName();
        }
        return fullName;
    }
}
