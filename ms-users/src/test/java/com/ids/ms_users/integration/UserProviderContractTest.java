package com.ids.ms_users.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ids.ms_users.dto.request.LoginRequest;
import com.ids.ms_users.dto.request.RegisterUserRequest;
import com.ids.ms_users.dto.response.TokenResponse;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifica que ms-users cumple como proveedor el contrato de GET /api/v1/users/{id}
 * (docs/contratos/ms-users.yaml): los campos de UserResponse y los códigos 200, 401, 403 y 404.
 * ms-users no consume servicios (excepción D13); esta prueba protege al consumidor futuro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserProviderContractTest {

    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String USERS_PATH = "/api/v1/users";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PASSWORD = "Password123";
    private static final String ADMIN_EMAIL = "admin-test@example.com";
    private static final String ADMIN_PASSWORD = "AdminTest123!";
    private static final long UNKNOWN_USER_ID = 999_999L;
    private static final Set<String> USER_RESPONSE_FIELDS =
        Set.of("id", "email", "fullName", "role", "active", "createdAt");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getUserByIdReturnsExactlyTheContractFields() throws Exception {
        String email = "contract-user@example.com";
        long userId = register(email);
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken();

        MvcResult result = mockMvc.perform(get(USERS_PATH + "/" + userId)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("SOLICITANTE"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();

        Map<String, Object> fields = objectMapper.readValue(
            result.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() { });
        assertThat(fields.keySet()).containsExactlyInAnyOrderElementsOf(USER_RESPONSE_FIELDS);
    }

    @Test
    void getUserByIdReturns404ProblemDetailWhenUserDoesNotExist() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).accessToken();

        mockMvc.perform(get(USERS_PATH + "/" + UNKNOWN_USER_ID)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void getUserByIdReturns401WithoutToken() throws Exception {
        mockMvc.perform(get(USERS_PATH + "/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getUserByIdReturns403ForNonAdmin() throws Exception {
        String email = "contract-solicitante@example.com";
        long userId = register(email);
        String token = login(email, PASSWORD).accessToken();

        mockMvc.perform(get(USERS_PATH + "/" + userId)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private long register(String email) throws Exception {
        RegisterUserRequest request = new RegisterUserRequest("Usuario de Prueba", email, PASSWORD);
        MvcResult result = mockMvc.perform(post(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private TokenResponse login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        ResultActions actions = mockMvc.perform(post(AUTH_PATH + "/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
        MvcResult result = actions.andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }
}
