package com.ids.ms_users.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ids.ms_users.config.SecurityConfig;

@WebMvcTest(controllers = UnmappedRouteSecurityTest.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class,
        ProblemAccessDeniedHandler.class, JwtRolesConverter.class,
        UnmappedRouteAuthorizationManager.class})
class UnmappedRouteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousRequestToUnmappedRouteReturnsRouteNotMapped401() throws Exception {
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_MAPPED"));
    }

    @Test
    void authenticatedRequestToUnmappedRouteReturns404() throws Exception {
        mockMvc.perform(get("/no-existe").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
