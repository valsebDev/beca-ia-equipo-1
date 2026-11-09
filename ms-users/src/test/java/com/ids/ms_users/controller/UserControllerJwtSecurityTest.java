package com.ids.ms_users.controller;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.UpdateUserStatusRequest;
import com.ids.ms_users.dto.UserResponse;
import com.ids.ms_users.exception.GlobalExceptionHandler;
import com.ids.ms_users.exception.ForbiddenOperationException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.service.TokenService;
import com.ids.ms_users.service.UserService;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TokenService.class})
class UserControllerJwtSecurityTest {

    private static final String USER_IDENTIFIER = "c2bc24d7-923d-4383-9425-c3b1573dd005";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private TokenService tokenService;

        private final JsonMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private UserService userService;

    @Test
    void tokenAdminValido_actualizaEstado_yResponde200() throws Exception {
        String targetIdentifier = "uuid-target";
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        given(userService.updateStatusByAdmin(
            eq(targetIdentifier), any(UpdateUserStatusRequest.class), eq(List.of("ADMIN"))))
                .willReturn(response(targetIdentifier, false));

        mockMvc.perform(patch("/api/v1/users/{userIdentifier}/status", targetIdentifier)
                        .header("Authorization", "Bearer " + tokenFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAlterado_responde401() throws Exception {
        String[] parts = tokenFor(Role.ADMIN).split("\\.");
        String payload = parts[1];
        parts[1] = (payload.charAt(0) == 'A' ? "B" : "A") + payload.substring(1);
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch("/api/v1/users/{userIdentifier}/status", "uuid-target")
                        .header("Authorization", "Bearer " + String.join(".", parts))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAusente_responde401() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch("/api/v1/users/{userIdentifier}/status", "uuid-target")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioNormal_responde403() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        given(userService.updateStatusByAdmin(
            eq("uuid-target"), any(UpdateUserStatusRequest.class), eq(List.of("SOLICITANTE"))))
            .willThrow(new ForbiddenOperationException("Solo ADMIN puede cambiar estados"));

        mockMvc.perform(patch("/api/v1/users/{userIdentifier}/status", "uuid-target")
                        .header("Authorization", "Bearer " + tokenFor(Role.SOLICITANTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNoExistente_conTokenAdmin_responde404() throws Exception {
        String targetIdentifier = "uuid-no-existe";
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        given(userService.updateStatusByAdmin(
            eq(targetIdentifier), any(UpdateUserStatusRequest.class), eq(List.of("ADMIN"))))
                .willThrow(new UserNotFoundException("Usuario no encontrado con identificador: " + targetIdentifier));

        mockMvc.perform(patch("/api/v1/users/{userIdentifier}/status", targetIdentifier)
                        .header("Authorization", "Bearer " + tokenFor(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private String tokenFor(Role role) {
        User user = new User(USER_IDENTIFIER, "Ana", "García", "López", "ana@example.com",
                "encoded-password", role, true, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "id", 42L);
        return tokenService.generateToken(user);
    }

    private UserResponse response(String userIdentifier, boolean active) {
        return new UserResponse(userIdentifier, "Pedro", "Navaja", null, "pedro@ids.com",
                Role.AGENTE, active, LocalDateTime.now());
    }
}
