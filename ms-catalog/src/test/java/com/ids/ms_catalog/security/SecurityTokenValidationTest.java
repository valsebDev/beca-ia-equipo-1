package com.ids.ms_catalog.security;

import com.ids.ms_catalog.config.JwtDecoderConfig;
import com.ids.ms_catalog.config.SecurityConfig;
import com.ids.ms_catalog.controller.CategoryController;
import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static com.ids.ms_catalog.security.SecurityTestSupport.ADMIN_ROLE;
import static com.ids.ms_catalog.security.SecurityTestSupport.AUDIENCE;
import static com.ids.ms_catalog.security.SecurityTestSupport.ISSUER;
import static com.ids.ms_catalog.security.SecurityTestSupport.TEST_SECRET;
import static com.ids.ms_catalog.security.SecurityTestSupport.signedToken;
import static com.ids.ms_catalog.security.SecurityTestSupport.validToken;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtDecoderConfig.class})
@ActiveProfiles("test")
class SecurityTokenValidationTest {

	private static final String BASE_PATH = "/api/v1/categories";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String BODY = "{\"name\":\"Redes\",\"description\":\"Problemas de red\"}";
	private static final String CODE_PATH = "$.code";
	private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
	private static final String OTHER_ISSUER = "ms-otro";
	private static final String OTHER_AUDIENCE = "otra-audiencia";
	private static final int SECRET_LENGTH_BYTES = 32;
	private static final long FUTURE_SECONDS = 60;
	private static final long EXPIRED_AGO_SECONDS = 3600;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@Autowired
	SecurityTokenValidationTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	static Stream<Arguments> tokensInvalidos() {
		Instant future = Instant.now().plusSeconds(FUTURE_SECONDS);
		List<String> roles = List.of(ADMIN_ROLE);
		return Stream.of(
				Arguments.of("firma alterada", signedToken(randomSecret(), ISSUER, AUDIENCE, future, roles)),
				Arguments.of("expirado", signedToken(TEST_SECRET, ISSUER, AUDIENCE,
						Instant.now().minusSeconds(EXPIRED_AGO_SECONDS), roles)),
				Arguments.of("iss incorrecto", signedToken(TEST_SECRET, OTHER_ISSUER, AUDIENCE, future, roles)),
				Arguments.of("aud incorrecto", signedToken(TEST_SECRET, ISSUER, OTHER_AUDIENCE, future, roles)));
	}

	private static String randomSecret() {
		byte[] secret = new byte[SECRET_LENGTH_BYTES];
		SECURE_RANDOM.nextBytes(secret);
		return Base64.getEncoder().encodeToString(secret);
	}

	@Test
	void sinTokenDevuelve401ConProblemDetail() throws Exception {
		mockMvc.perform(get(BASE_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath(CODE_PATH).value(UNAUTHORIZED_CODE));
		verifyNoInteractions(categoryService);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("tokensInvalidos")
	void tokenInvalidoDevuelve401(String descripcion, String token) throws Exception {
		mockMvc.perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath(CODE_PATH).value(UNAUTHORIZED_CODE));
		verifyNoInteractions(categoryService);
	}

	@Test
	void tokenValidoPermiteLecturaConElRolDelClaim() throws Exception {
		when(categoryService.findAll(any())).thenReturn(
				List.of(new CategoryResponse(1L, "Software", null, true)));

		mockMvc.perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION,
						bearer(validToken(List.of("SOLICITANTE")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Software"));
	}

	@Test
	void rolAdminDelClaimHabilitaLaCreacion() throws Exception {
		when(categoryService.create(any())).thenReturn(new CategoryResponse(7L, "Redes", "Problemas de red", true));

		mockMvc.perform(post(BASE_PATH).contentType(JSON).content(BODY)
						.header(HttpHeaders.AUTHORIZATION, bearer(validToken(List.of(ADMIN_ROLE)))))
				.andExpect(status().isCreated());
	}

	@Test
	void rolSolicitanteDelClaimNoPuedeCrear() throws Exception {
		mockMvc.perform(post(BASE_PATH).contentType(JSON).content(BODY)
						.header(HttpHeaders.AUTHORIZATION, bearer(validToken(List.of("SOLICITANTE")))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath(CODE_PATH).value("FORBIDDEN"));
		verifyNoInteractions(categoryService);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
