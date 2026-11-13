package com.ids.ms_users.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.request.LogoutRequest;
import com.ids.ms_users.dto.request.RefreshRequest;
import com.ids.ms_users.dto.request.RegisterUserRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityFlowIntegrationTest {

    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String HEALTH_PATH = "/actuator/health";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PASSWORD = "Password123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthIsPublicAndHidesDetails() throws Exception {
        mockMvc.perform(get(HEALTH_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void logoutRevokesOnlyTheCallersOwnRefreshToken() throws Exception {
        String ownerEmail = "owner-logout@example.com";
        String otherEmail = "other-logout@example.com";
        register(ownerEmail);
        register(otherEmail);
        TokenResponse owner = login(ownerEmail);
        String otherRefreshToken = login(otherEmail).refreshToken();

        logout(owner.accessToken(), otherRefreshToken).andExpect(status().isNoContent());
        refresh(otherRefreshToken).andExpect(status().isOk());

        logout(owner.accessToken(), owner.refreshToken()).andExpect(status().isNoContent());
        refresh(owner.refreshToken()).andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        RegisterUserRequest request = new RegisterUserRequest("Usuario de Prueba", email, PASSWORD);
        mockMvc.perform(post(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private TokenResponse login(String email) throws Exception {
        LoginRequest request = new LoginRequest(email, PASSWORD);
        MvcResult result = mockMvc.perform(post(AUTH_PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post(AUTH_PATH + "/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))));
    }

    private ResultActions logout(String accessToken, String refreshToken) throws Exception {
        return mockMvc.perform(post(AUTH_PATH + "/logout")
            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))));
    }
}
