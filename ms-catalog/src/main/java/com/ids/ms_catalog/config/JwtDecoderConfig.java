package com.ids.ms_catalog.config;

import com.ids.ms_catalog.config.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtDecoderConfig {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		SecretKeySpec key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(properties.issuer()),
				audienceValidator(properties.audience())));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
		return new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
				aud -> aud != null && aud.contains(audience));
	}
}
