package com.ids.ms_users.controller;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ids.ms_users.config.SecurityConfig;
import com.ids.ms_users.security.JwtRolesConverter;
import com.ids.ms_users.security.ProblemAccessDeniedHandler;
import com.ids.ms_users.security.ProblemAuthenticationEntryPoint;
import com.ids.ms_users.security.UnmappedRouteAuthorizationManager;
import com.ids.ms_users.service.UserService;

import static org.mockito.Mockito.when;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class,
        ProblemAccessDeniedHandler.class, JwtRolesConverter.class,
        UnmappedRouteAuthorizationManager.class})
class UserControllerSecurityTest {

    private static final String USER_ROLE = "ROLE_USER";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String CREATE_USER_BODY = """
            {"name":"Ana","paternalName":"Lopez","email":"ana@ids.com",
             "password":"Passw0rd!","role":"AGENTE"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void meWithInvalidTokenReturns401() throws Exception {
        doThrow(new BadJwtException("firma invalida")).when(jwtDecoder).decode("invalido");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithUserRoleReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(j -> j.subject("1"))
                                .authorities(new SimpleGrantedAuthority(USER_ROLE))))
                .andExpect(status().isOk());
    }

    @Test
    void listUsersWithUserRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority(USER_ROLE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));
    }

    @Test
    void listUsersWithAdminRoleReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ADMIN_ROLE))))
                .andExpect(status().isOk());
    }

    @Test
    void listUsersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserByIdWithUserRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/5")
                        .with(jwt().authorities(new SimpleGrantedAuthority(USER_ROLE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdWithAdminRoleReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/users/5")
                        .with(jwt().authorities(new SimpleGrantedAuthority(ADMIN_ROLE))))
                .andExpect(status().isOk());
    }

    @Test
    void createUserWithUserRoleReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_USER_BODY)
                        .with(jwt().authorities(new SimpleGrantedAuthority(USER_ROLE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatusWithUserRoleReturns403() throws Exception {
        mockMvc.perform(patch("/api/v1/users/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}")
                        .with(jwt().authorities(new SimpleGrantedAuthority(USER_ROLE))))
                .andExpect(status().isForbidden());
    }
}
