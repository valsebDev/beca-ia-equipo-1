package com.ids.ms_users.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ids.ms_users.config.JwtDecoderConfig;
import com.ids.ms_users.config.JwtEncoderConfig;
import com.ids.ms_users.config.SecurityConfig;
import com.ids.ms_users.security.JwtRolesConverter;
import com.ids.ms_users.security.ProblemAccessDeniedHandler;
import com.ids.ms_users.security.ProblemAuthenticationEntryPoint;
import com.ids.ms_users.security.UnmappedRouteAuthorizationManager;
import com.ids.ms_users.service.UserService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtEncoderConfig.class, JwtDecoderConfig.class,
        ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class,
        JwtRolesConverter.class, UnmappedRouteAuthorizationManager.class})
class UserControllerSecurityTest {

    private static final String ME_PATH = "/api/v1/users/me";
    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_BY_ID_PATH = "/api/v1/users/uid-5";
    private static final String STATUS_PATH = "/api/v1/users/uid-5/status";
    private static final String SOLICITANTE = "SOLICITANTE";
    private static final String AGENTE = "AGENTE";
    private static final String ADMIN = "ADMIN";
    private static final String FORBIDDEN_SECRET = "otro-secreto-de-prueba-con-mas-de-32-bytes!!";
    private static final String UPDATE_PROFILE_BODY = """
            {"name":"Ana","paternalName":"Lopez","maternalName":"Ruiz"}""";
    private static final String CREATE_USER_BODY = """
            {"user":{"name":"Ana","paternalName":"Lopez","email":"ana@ids.com",
             "password":"Passw0rd!"},"role":"AGENTE"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.issuer}")
    private String issuer;

    @Value("${app.security.jwt.audience}")
    private String audience;

    @MockitoBean
    private UserService userService;

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void meWithBadSignatureReturns401() throws Exception {
        String token = encode(claims(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, audience),
                forgedEncoder());

        mockMvc.perform(get(ME_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithExpiredTokenReturns401() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().minusSeconds(60), issuer, audience);

        mockMvc.perform(get(ME_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithWrongIssuerReturns401() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), "otro-emisor", audience);

        mockMvc.perform(get(ME_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithWrongAudienceReturns401() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, "otra-audiencia");

        mockMvc.perform(get(ME_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithSolicitanteReturns200() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(ME_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void updateMeWithAgenteReturns200() throws Exception {
        String token = token(AGENTE, 2L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(put(ME_PATH)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_PROFILE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void listUsersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(USERS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsersWithSolicitanteReturns403() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(USERS_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));
    }

    @Test
    void listUsersWithAgenteReturns403() throws Exception {
        String token = token(AGENTE, 2L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(USERS_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsersWithAdminReturns200() throws Exception {
        String token = token(ADMIN, 3L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(USERS_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void createUserWithSolicitanteReturns403() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(post(USERS_PATH)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserWithAdminReturns201() throws Exception {
        String token = token(ADMIN, 3L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(post(USERS_PATH)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void getUserByIdentifierWithSolicitanteReturns403() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(USER_BY_ID_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdentifierWithAgenteReturns403() throws Exception {
        String token = token(AGENTE, 2L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(USER_BY_ID_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdentifierWithAdminReturns200() throws Exception {
        String token = token(ADMIN, 3L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(get(USER_BY_ID_PATH).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatusWithSolicitanteReturns403() throws Exception {
        String token = token(SOLICITANTE, 1L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(patch(STATUS_PATH)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatusWithAdminReturns200() throws Exception {
        String token = token(ADMIN, 3L, Instant.now().plusSeconds(60), issuer, audience);

        mockMvc.perform(patch(STATUS_PATH)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true}"))
                .andExpect(status().isOk());
    }

    private String token(String role, long subject, Instant expiresAt, String iss, String aud) {
        return encode(claims(role, subject, expiresAt, iss, aud), jwtEncoder);
    }

    private JwtClaimsSet claims(String role, long subject, Instant expiresAt, String iss, String aud) {
        return JwtClaimsSet.builder()
                .issuer(iss)
                .audience(List.of(aud))
                .subject(String.valueOf(subject))
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of(role))
                .build();
    }

    private static String encode(JwtClaimsSet claims, JwtEncoder encoder) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static JwtEncoder forgedEncoder() {
        SecretKey key = new SecretKeySpec(FORBIDDEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
