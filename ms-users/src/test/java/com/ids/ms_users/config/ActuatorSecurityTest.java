package com.ids.ms_users.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ids.ms_users.controller.AuthController;
import com.ids.ms_users.security.JwtRolesConverter;
import com.ids.ms_users.security.ProblemAccessDeniedHandler;
import com.ids.ms_users.security.ProblemAuthenticationEntryPoint;
import com.ids.ms_users.security.UnmappedRouteAuthorizationManager;
import com.ids.ms_users.service.AuthService;
import com.ids.ms_users.service.UserService;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtEncoderConfig.class, JwtDecoderConfig.class,
        ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class,
        JwtRolesConverter.class, UnmappedRouteAuthorizationManager.class})
class ActuatorSecurityTest {

    private static final String ENV_PATH = "/actuator/env";
    private static final String BEANS_PATH = "/actuator/beans";

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

    @MockitoBean
    private AuthService authService;

    @Test
    void otherActuatorEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get(ENV_PATH))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BEANS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unexposedActuatorEndpointsReturnNotFoundWhenAuthenticated() throws Exception {
        String token = token(Instant.now().plusSeconds(900));

        mockMvc.perform(get(ENV_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String token(Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("1")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of("USER"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
