package com.ids.ms_users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ids.ms_users.domain.Role;
import com.ids.ms_users.dto.LoginRequest;
import com.ids.ms_users.dto.RegisterUserRequest;
import com.ids.ms_users.dto.UpdateUserStatusRequest;
import com.ids.ms_users.dto.UserResponse;
import com.ids.ms_users.exception.AccountLockedException;
import com.ids.ms_users.exception.EmailAlreadyExistsException;
import com.ids.ms_users.exception.ForbiddenOperationException;
import com.ids.ms_users.exception.GlobalExceptionHandler;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.exception.UserNotFoundException;
import com.ids.ms_users.service.TokenService;
import com.ids.ms_users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UserController userController;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController, userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("HU-01: POST /api/v1/auth/register - Exitoso devuelve 201 Created")
    void register_payloadValido_retorna201Created() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Maria", "Gonzalez", "Perez", "maria@ids.com", "Password#2026"
        );
        UserResponse response = new UserResponse(
                "uuid-maria", "Maria", "Gonzalez", "Perez", "maria@ids.com",
                Role.SOLICITANTE, true, LocalDateTime.now()
        );
        given(userService.register(any(RegisterUserRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@ids.com"))
                .andExpect(jsonPath("$.userIdentifier").value("uuid-maria"));
    }

    @Test
    @DisplayName("HU-01: POST /api/v1/auth/register - Correo duplicado devuelve 409 Conflict")
    void register_correoDuplicado_retorna409Conflict() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Maria", "Gonzalez", "Perez", "duplicado@ids.com", "Password#2026"
        );
        given(userService.register(any(RegisterUserRequest.class)))
                .willThrow(new EmailAlreadyExistsException("duplicado@ids.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de Datos"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("HU-01: POST /api/v1/auth/register - Payload sin campos obligatorios devuelve 400 Bad Request")
    void register_payloadInvalido_retorna400BadRequest() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "", "", "", "formato-invalido", "123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de Validación"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("HU-02: POST /api/v1/auth/login - Credenciales inválidas devuelven 401 Unauthorized")
    void login_credencialesInvalidas_devuelve401Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest("user@ids.com", "BadPassword");
        given(userService.login(any(LoginRequest.class)))
                .willThrow(new InvalidCredentialsException("Credenciales no válidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Credenciales Inválidas"));
    }

    @Test
    @DisplayName("HU-03: POST /api/v1/auth/login - Cuenta bloqueada devuelve 423 Locked")
    void login_cuentaBloqueada_devuelve423Locked() throws Exception {
        LoginRequest request = new LoginRequest("bloqueado@ids.com", "Password#123");
        given(userService.login(any(LoginRequest.class)))
                .willThrow(new AccountLockedException("Cuenta bloqueada temporalmente"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.title").value("Cuenta Bloqueada Temporalmente"));
    }

    @Test
    @DisplayName("HU-04: GET /api/v1/users/me - Consulta de perfil exitosa devuelve 200 OK")
    void getMyProfile_usuarioAutenticado_devuelve200Ok() throws Exception {
        String tokenHeader = "Bearer mock-token";
        String userIdentifier = "uuid-solicitante";
        UserResponse response = new UserResponse(
                userIdentifier, "Laura", "Torres", null, "laura@ids.com",
                Role.SOLICITANTE, true, LocalDateTime.now()
        );

        given(tokenService.extractUserIdentifier(tokenHeader)).willReturn(userIdentifier);
        given(userService.getProfile(userIdentifier)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", tokenHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userIdentifier").value(userIdentifier))
                .andExpect(jsonPath("$.name").value("Laura"));
    }

    @Test
    @DisplayName("HU-04: GET /api/v1/users/me - Usuario no encontrado devuelve 404 Not Found")
    void getMyProfile_usuarioNoEncontrado_devuelve404NotFound() throws Exception {
        String tokenHeader = "Bearer mock-token";
        String userIdentifier = "uuid-no-existe";

        given(tokenService.extractUserIdentifier(tokenHeader)).willReturn(userIdentifier);
        given(userService.getProfile(userIdentifier))
                .willThrow(new UserNotFoundException("Usuario no encontrado con identificador: " + userIdentifier));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", tokenHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Usuario No Encontrado"));
    }

    @Test
        @DisplayName("HU-USR-05: PATCH /api/v1/users/{id}/status - ADMIN actualiza estado")
        void updateUserStatusByAdmin_adminActualizaEstado_devuelve200Ok() throws Exception {
        String tokenHeader = "Bearer mock-token-admin";
        String userIdentifier = "uuid-target";
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);
        UserResponse response = new UserResponse(
                userIdentifier, "Pedro", "Navaja", null, "pedro@ids.com",
                Role.AGENTE, false, LocalDateTime.now()
        );

        given(tokenService.extractRoles(tokenHeader)).willReturn(List.of("ADMIN"));
        given(userService.updateStatusByAdmin(
                eq(userIdentifier), any(UpdateUserStatusRequest.class), eq(List.of("ADMIN"))))
                .willReturn(response);

        mockMvc.perform(patch("/api/v1/users/{userIdentifier}/status", userIdentifier)
                        .header("Authorization", tokenHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - ADMIN obtiene un usuario por identificador (200 OK)")
    void getUserByIdByAdmin_adminConsultaUsuario_devuelve200Ok() throws Exception {
        String tokenHeader = "******";
        String userIdentifier = "uuid-target";
        UserResponse response = new UserResponse(
                userIdentifier, "Pedro", "Navaja", null, "pedro@ids.com",
                Role.AGENTE, true, LocalDateTime.now()
        );

        given(tokenService.extractRoles(tokenHeader)).willReturn(List.of("ADMIN"));
        given(userService.getUserByIdByAdmin(userIdentifier, List.of("ADMIN"))).willReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", userIdentifier)
                        .header("Authorization", tokenHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userIdentifier").value(userIdentifier))
                .andExpect(jsonPath("$.email").value("pedro@ids.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - usuario inexistente devuelve 404 Not Found")
    void getUserByIdByAdmin_usuarioNoEncontrado_devuelve404NotFound() throws Exception {
        String tokenHeader = "******";
        String userIdentifier = "uuid-no-existe";

        given(tokenService.extractRoles(tokenHeader)).willReturn(List.of("ADMIN"));
        given(userService.getUserByIdByAdmin(userIdentifier, List.of("ADMIN")))
                .willThrow(new UserNotFoundException("Usuario no encontrado con identificador: " + userIdentifier));

        mockMvc.perform(get("/api/v1/users/{id}", userIdentifier)
                        .header("Authorization", tokenHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - rol distinto de ADMIN devuelve 403 Forbidden")
    void getUserByIdByAdmin_sinRolAdmin_devuelve403Forbidden() throws Exception {
        String tokenHeader = "******";
        String userIdentifier = "uuid-target";

        given(tokenService.extractRoles(tokenHeader)).willReturn(List.of("SOLICITANTE"));
        given(userService.getUserByIdByAdmin(userIdentifier, List.of("SOLICITANTE")))
                .willThrow(new ForbiddenOperationException("Solo un usuario con rol ADMIN puede cambiar estados"));

        mockMvc.perform(get("/api/v1/users/{id}", userIdentifier)
                        .header("Authorization", tokenHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));
    }
}