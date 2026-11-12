package com.ids.ms_users.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.ids.ms_users.config.properties.JwtProperties;
import com.ids.ms_users.domain.User;

@Service
public class TokenService {

    private static final String CLAIM_ROLES = "roles";

    private final JwtEncoder jwtEncoder;
    private final Duration accessTtl;
    private final String issuer;
    private final String audience;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.accessTtl = properties.accessTtl();
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
                .expiresAt(now.plus(accessTtl))
                .claim(CLAIM_ROLES, List.of(user.getRole().name()))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getAccessTtlSeconds() {
        return accessTtl.getSeconds();
    }
}
