package com.ids.ms_users.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
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

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtEncoderConfig.class, JwtDecoderConfig.class,
        ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class,
        JwtRolesConverter.class, UnmappedRouteAuthorizationManager.class})
class UserControllerJwtAuthorizationTest {

    private static final String ME_PATH = "/api/v1/users/me";
    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_BY_ID_PATH = "/api/v1/users/5";
    private static final String STATUS_PATH = "/api/v1/users/5/status";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SOLICITANTE = "SOLICITANTE";
    private static final String AGENTE = "AGENTE";
    private static final String ADMIN = "ADMIN";
    private static final String SUBJECT = "1";
    private static final String CREATE_USER_BODY = """
            {"fullName":"Ana Lopez","email":"ana@ids.com","password":"Passw0rd!","role":"AGENTE"}""";
    private static final String STATUS_BODY = "{\"active\":true}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void meWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithSolicitanteReturns200() throws Exception {
        mockMvc.perform(get(ME_PATH).with(jwtWith(SOLICITANTE)))
                .andExpect(status().isOk());
    }

    @Test
    void listUsersWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(USERS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsersWithSolicitanteReturns403() throws Exception {
        mockMvc.perform(get(USERS_PATH).with(jwtWith(SOLICITANTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsersWithAgenteReturns403() throws Exception {
        mockMvc.perform(get(USERS_PATH).with(jwtWith(AGENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsersWithAdminReturns200() throws Exception {
        mockMvc.perform(get(USERS_PATH).with(jwtWith(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void createUserWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUserWithAgenteReturns403() throws Exception {
        mockMvc.perform(post(USERS_PATH)
                        .with(jwtWith(AGENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserWithAdminReturns201() throws Exception {
        mockMvc.perform(post(USERS_PATH)
                        .with(jwtWith(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void getUserByIdWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(USER_BY_ID_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserByIdWithSolicitanteReturns403() throws Exception {
        mockMvc.perform(get(USER_BY_ID_PATH).with(jwtWith(SOLICITANTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdWithAdminReturns200() throws Exception {
        mockMvc.perform(get(USER_BY_ID_PATH).with(jwtWith(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatusWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(patch(STATUS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATUS_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStatusWithAgenteReturns403() throws Exception {
        mockMvc.perform(patch(STATUS_PATH)
                        .with(jwtWith(AGENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATUS_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatusWithAdminReturns200() throws Exception {
        mockMvc.perform(patch(STATUS_PATH)
                        .with(jwtWith(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATUS_BODY))
                .andExpect(status().isOk());
    }

    private static JwtRequestPostProcessor jwtWith(String role) {
        return jwt().jwt(builder -> builder.subject(SUBJECT))
                .authorities(new SimpleGrantedAuthority(ROLE_PREFIX + role));
    }
}
