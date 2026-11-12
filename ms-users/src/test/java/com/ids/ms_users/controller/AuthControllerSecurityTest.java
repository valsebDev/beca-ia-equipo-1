package com.ids.ms_users.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ids.ms_users.config.JwtDecoderConfig;
import com.ids.ms_users.config.JwtEncoderConfig;
import com.ids.ms_users.config.SecurityConfig;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.security.JwtRolesConverter;
import com.ids.ms_users.security.ProblemAccessDeniedHandler;
import com.ids.ms_users.security.ProblemAuthenticationEntryPoint;
import com.ids.ms_users.security.UnmappedRouteAuthorizationManager;
import com.ids.ms_users.service.UserService;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtEncoderConfig.class, JwtDecoderConfig.class,
        ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class,
        JwtRolesConverter.class, UnmappedRouteAuthorizationManager.class})
class AuthControllerSecurityTest {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String LOGIN_BODY = "{\"email\":\"ana@ids.com\",\"password\":\"Passw0rd!\"}";
    private static final String REFRESH_BODY = "{\"refreshToken\":\"rt-123\"}";
    private static final String LOGOUT_BODY = "{\"refreshToken\":\"rt-123\"}";
    private static final String REGISTER_BODY =
            "{\"fullName\":\"Ana Lopez\",\"email\":\"ana@ids.com\",\"password\":\"Passw0rd!\"}";
    private static final String WEAK_REGISTER_BODY =
            "{\"fullName\":\"Ana Lopez\",\"email\":\"ana@ids.com\",\"password\":\"solodeletras\"}";

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
    void loginWithValidCredentialsReturns200() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongCredentialsReturns401() throws Exception {
        doThrow(new InvalidCredentialsException("Credenciales inválidas")).when(userService)
                .login(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithInactiveAccountReturns401WithSameBodyAsWrongCredentials() throws Exception {
        doThrow(new InvalidCredentialsException("Credenciales inválidas")).when(userService)
                .login(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshWithoutAccessTokenIsAllowedAndReturns200() throws Exception {
        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void logoutWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(LOGOUT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGOUT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithValidTokenReturns204() throws Exception {
        String token = token("SOLICITANTE", Instant.now().plusSeconds(60));

        mockMvc.perform(post(LOGOUT_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGOUT_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutWithAgenteReturns204() throws Exception {
        String token = token("AGENTE", Instant.now().plusSeconds(60));

        mockMvc.perform(post(LOGOUT_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGOUT_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutWithAdminReturns204() throws Exception {
        String token = token("ADMIN", Instant.now().plusSeconds(60));

        mockMvc.perform(post(LOGOUT_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGOUT_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void registerIsPublicAndReturns201WithoutToken() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void registerWithWeakPasswordReturns400() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WEAK_REGISTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void refreshWithInvalidTokenReturns401() throws Exception {
        doThrow(new InvalidCredentialsException("Credenciales inválidas")).when(userService)
                .refresh(anyString());

        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void registerWithMalformedJsonReturns400MalformedRequest() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.type").value("https://mesa-ayuda.ids/problems/malformed-request"));
    }

    @Test
    void registerWithUnexpectedErrorReturns500WithoutLeakingInternalDetail() throws Exception {
        doThrow(new IllegalStateException("fallo interno con datos sensibles")).when(userService)
                .register(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post(REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.type").value("https://mesa-ayuda.ids/problems/internal-server-error"))
                .andExpect(jsonPath("$.detail").value("Ha ocurrido un error inesperado en el servidor"));
    }

    private String token(String role, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("1")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
