# PLAN — Seguridad de ms-users (P3)

Decisiones tomadas: D1 seguridad + endpoints faltantes de ARCH; D2 migrar a Nimbus y quitar JJWT; D3 ARCHITECTURE prevalece (`sub` numérico, rutas `{id}`); D4 login inactivo → 401 genérico; D5 migrar tests a Testcontainers; bloqueo por intentos fallidos retirado (HU-USR-03, ARCH no lo contempla).

## 1. Estado actual (verificado)
- Sin `SecurityFilterChain`; pom con `spring-security-crypto` + JJWT 0.12.6. `UserController` parsea el header con `TokenService` y `UserService.requireAdmin` valida el rol.
- Endpoints: register, login, `GET /users/me`, `GET /users/{id}` (UUID), `PATCH /users/{userIdentifier}/status`. Faltan: `/auth/refresh`, `/auth/logout`, `GET /users`, `POST /users`, ADMIN inicial.
- Token: `iss=ms-users`, `aud=mesa-ayuda`, `sub`=id numérico, `userIdentifier`, `email`, `name`, `roles`, `iat`, `exp`; sin `jti`.
- Tests: `@WebMvcTest(addFilters=false)` y H2 (`create-drop`), contra reglas 18/22.
- `login()` es `@Transactional` y lanza excepción tras `save()` → el rollback revierte `failedAttempts/lockedUntil` (HU-USR-03 no persistiría).
- `validateAccountStatus` corre antes de verificar contraseña (enumeración de cuentas).

## 2. Dependencias (confirma antes de tocar el pom — regla 2)
| Acción | Artefacto |
|---|---|
| + | `spring-boot-starter-security-oauth2-resource-server` |
| + test | `spring-boot-starter-security-test`, `spring-boot-testcontainers`, `testcontainers-postgresql` (+ `testcontainers-junit-jupiter` si Boot 4 lo requiere) |
| − | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` |
| − test | `h2` |
| = | `spring-security-crypto` |

## 3. Clases
Nuevas:
- `config/security/SecurityConfig`: stateless, CSRF off, `oauth2ResourceServer().jwt()`, `@EnableMethodSecurity`. Públicos: `POST /api/v1/auth/{register,login,refresh}`, `GET /actuator/health`. Beans `JwtDecoder` (HS256 + validadores `iss`, `aud`, `exp`), `JwtEncoder`, `PasswordEncoder` (BCrypt costo `BCRYPT_COST`).
- `config/security/JwtRolesConverter` (`roles` → `ROLE_*`), `ProblemAuthenticationEntryPoint` (401 `UNAUTHORIZED`), `ProblemAccessDeniedHandler` (403 `FORBIDDEN`).
- `config/security/AdminBootstrap`: crea el ADMIN inicial desde `ADMIN_EMAIL`/`ADMIN_PASSWORD` (sin default) solo si no existe ninguno.
- Refresh: `domain/RefreshToken`, `repository/RefreshTokenRepository`, `service/RefreshTokenService` (32 bytes `SecureRandom`, Base64URL, SHA-256, rotación, reutilización revoca todos), `dto/request/RefreshRequest`, `dto/request/CreateUserRequest`, `dto/response/TokenResponse`.
- Migración `V2__refresh_tokens.sql` (tabla `refresh_tokens` según ARCH §2.1; nunca editar V1).
- `exception/InvalidRefreshTokenException` (o reutilizar `InvalidCredentialsException`).

Modificadas:
- `TokenService`: emisión Nimbus + `jti`; se eliminan métodos de parseo manual.
- `UserController`: `@PreAuthorize` ADMIN en `GET /users`, `GET /users/{id}`, `POST /users`, `PATCH /users/{id}/status`; `/me` usa `@AuthenticationPrincipal Jwt` (`sub`). `AuthController`: `refresh` y `logout` (autenticado).
- `UserService`: sin `requireAdmin`/roles; login con orden HU-USR-02 (contraseña 401 → inactivo 401 genérico, sin bloqueo por intentos); desactivar revoca refresh tokens.
- `GlobalExceptionHandler`: `AccessDeniedException` → 403; mapear `code` según §3.4.
- `application.yaml`: Actuator solo `health`, `show-details: never`; `app.security.bcrypt-cost`, `refresh-ttl-days`; plantillas, `.env.example`, README, `docs/SECURITY-REPORT.md`, `docs/contratos/ms-users.yaml` (`bearerAuth`, 401/403).

## 4. Matriz endpoint × rol
| Endpoint | Sin token | Token inválido/expirado | SOLICITANTE | AGENTE | ADMIN |
|---|---|---|---|---|---|
| POST `/auth/register` | 201 | 201 | — | — | — |
| POST `/auth/login` | 200 | 200 | — | — | — |
| POST `/auth/refresh` | 200 (refresh válido) / 401 | — | — | — | — |
| POST `/auth/logout` | 401 | 401 | 204 | 204 | 204 |
| GET `/users/me` | 401 | 401 | 200 | 200 | 200 |
| GET `/users?role=` | 401 | 401 | 403 | 403 | 200 |
| GET `/users/{id}` | 401 | 401 | 403 | 403 | 200 |
| POST `/users` | 401 | 401 | 403 | 403 | 201 |
| PATCH `/users/{id}/status` | 401 | 401 | 403 | 403 | 200 |
| GET `/actuator/health` | 200 | 200 | 200 | 200 | 200 |
| otro `/actuator/*` | 401 | 401 | 404 | 404 | 404 |
`/me` siempre usa el `sub`; no hay recurso "ajeno".

## 5. Pruebas (401 / 403 / éxito)
- `SecurityTokenValidationTest`: `JwtDecoder` real; sin token, firma alterada, expirado, `iss`/`aud` incorrectos → 401 ProblemDetail.
- `UserControllerSecurityTest` y `AuthControllerSecurityTest` (`@WebMvcTest` + `@Import(SecurityConfig)` + `jwt()`): matriz parametrizada {sin token, SOLICITANTE, AGENTE, ADMIN}; en 403 `verifyNoInteractions(service)`.
- `ActuatorSecurityTest`, `JwtRolesConverterTest`, `JwtPropertiesTest` (secret < 32 bytes).
- `TokenServiceTest`: round-trip emisión/decodificación; claims `roles`, `sub`, `jti`.
- `RefreshTokenServiceTest`, `UserServiceTest`: rotación, reutilización, revocación; login inactivo 401, desactivar revoca.
- `AdminBootstrapTest`: crea / no duplica.
- `MsUsersApplicationTests` + repositorio con Testcontainers `postgres:16` (`@ServiceConnection`), Flyway activo, `ddl-auto=validate`.
- Ajustar `UserControllerTest`, `UserControllerJwtSecurityTest` (sin `addFilters=false`). Cierre: `./mvnw -B verify` (JaCoCo ≥ 70 % total, service y controller).

## 5b. Alineación con ARCHITECTURE (feedback 1, 2 y 3)

### A. `ms-users/docs/requerimientos.md` (regla 14: se actualiza el documento para que coincida con ARCH, antes de tocar código)
| Sección | Hoy | Cambio |
|---|---|---|
| Stack/Fase | "Fase P1" | Agregar P3: JWT HS256 con Nimbus, access + refresh |
| §2 Entidad `Usuario` | UUID `id`, `email` 150, `password`, `nombre`+`apellido`, `rol`, `activo`, `intentosFallidos`, `bloqueadoHasta` | `id` Long; `email` ≤254 en minúsculas; `password_hash`; `full_name` (2..150); `role`; `active`. Sin campos de bloqueo (HU-USR-03 retirada). Nueva entidad `RefreshToken` (§2.1 ARCH) |
| RN-03 | claims `sub`, `email`, `role`, `iat`, `exp` | `iss=ms-users`, `aud=mesa-ayuda`, `sub` (id Long como texto), `email`, `name`, `roles` (arreglo), `iat`, `exp` (15 min), `jti` |
| Nuevas RN | — | RN-09 refresh opaco 7 días con rotación y revocación total ante reutilización; RN-10 logout revoca; RN-11 desactivar revoca refresh tokens; RN-12 ADMIN inicial desde `ADMIN_EMAIL/ADMIN_PASSWORD`; RN-13 contraseña 8..72 con letra y dígito |
| HU-USR-01 | `nombre, apellido` | `email`, `password`, `fullName` (RegisterRequest ARCH §3.1) |
| HU-USR-02 esc. 3 | cuenta inactiva → 403 | 401 genérico "Credenciales inválidas" (no revela estado); orden: contraseña 401 → inactivo 401 |
| HU-USR-04 | `PUT /users/me` nombre y apellido | Solo consulta: `GET /users/me` (200 / 401). No hay `PUT /users/me` (no está en ARCH §3.1 ni en el contrato) |
| HU-USR-05 | `PATCH /users/{userIdentifier}/status` con `isActive` | `PATCH /users/{id}/status` con `{"active": boolean}`; 404 por id inexistente |
| Nuevas HU | — | HU-USR-06 refresh con rotación (`POST /auth/refresh`); HU-USR-07 logout (`POST /auth/logout`, 204); HU-USR-08 ADMIN lista usuarios (`GET /users?role=`); HU-USR-09 ADMIN crea usuario con rol (`POST /users`, 201/409); cada una con escenarios 401/403/éxito |
| Preguntas abiertas 2, 3, 5 | abiertas | Cerradas: alta de AGENTE/ADMIN vía `POST /users` (D8); TTL 15 min + refresh 7 d (D5); reactivación con `PATCH status active=true` |
| Actualizar también | — | `docs/backlog.md` (enlaces HU), `docs/contratos/ms-users.yaml`, README, `ms-users/docs/*`, `docs/seguridad.md` (acuerdo del equipo) |

### B. Código y BD (cambios para alinear con ARCH §2.1, §3.1, §3.4)
- **Migración `V2__align_users_with_architecture.sql`** (no se edita V1; hoy `users` solo tiene la V1):
  - `users`: renombrar `is_active`→`active`, `password`→`password_hash` (VARCHAR 100), `role` VARCHAR(20), `email` VARCHAR(254) + índice único en `lower(email)`, agregar `full_name` (migrando `name || paternal_name || maternal_name`), `updated_at TIMESTAMPTZ`, `created_at` → `TIMESTAMPTZ`.
  - Eliminar `user_identifier`, `name`, `paternal_name`, `maternal_name`, `jwt_id`, `jwt_expires_at`, `refresh_token_hash`, `refresh_token_expires_at` y los índices asociados. Las columnas `failed_attempts` y `locked_until` se conservan sin mapear en JPA (no tienen uso; el bloqueo fue retirado).
  - `PK id BIGSERIAL` ya existe.
- **Migración `V3__refresh_tokens.sql`**: `refresh_tokens(id BIGSERIAL, user_id FK ON DELETE CASCADE, token_hash CHAR(64) UNIQUE, expires_at, revoked_at, created_at)`, `idx_refresh_tokens_user_id`.
- **Entidad `User`**: campos `fullName`, `active`, `passwordHash`, `createdAt/updatedAt` (`Instant`); sin `userIdentifier` ni columnas de JWT. `UserRepository`: `findByEmailIgnoreCase`, `existsByEmailIgnoreCase`, `findByRole`. Email se normaliza a minúsculas en el service.
- **DTOs (records)**: `RegisterRequest{email,password,fullName}`, `CreateUserRequest{...,role}`, `LoginRequest{email,password}` (hoy `softPassword`), `RefreshRequest{refreshToken}`, `UpdateStatusRequest{active}`, `UpdateProfileRequest{fullName}`, `UserResponse{id,email,fullName,role,active,createdAt}`, `TokenResponse{accessToken,tokenType,expiresIn,refreshToken,refreshExpiresIn}` (reemplaza `AuthResponse`). Bean Validation con tamaño máximo en todos los textos (regla 24); contraseña `@Size(8,72)` + `@Pattern` letra y dígito.
- **Rutas**: `/users/{id}` y `/users/{id}/status` con `Long`; `GlobalExceptionHandler` usa `code` de §3.4 (`UNAUTHORIZED`, `INVALID_CREDENTIALS`, `FORBIDDEN`, `USER_NOT_FOUND`, `USER_ALREADY_EXISTS`/`EMAIL_ALREADY_EXISTS`, `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INTERNAL_ERROR`); `type` con base `https://mesa-ayuda.ids/problems/...`, `instance` con la ruta, `errors` solo en 400. Cuenta bloqueada: retirada (D5 de `docs/seguridad.md`; ARCH no define bloqueo por intentos fallidos), por lo que no existe 423 `ACCOUNT_LOCKED`.
- **Mapper**: `UserMapper` (métodos `static`) actualizado a los nuevos campos.
- **Tests**: se reescriben datos y rutas en `UserControllerTest`, `UserControllerJwtSecurityTest`, `UserServiceTest`, `TokenServiceTest` (no se debilitan aserciones; el cambio de contrato justifica la actualización, regla 20, y se anota en la bitácora).

### C. Cumplimiento de calidad (límites 30 líneas/método, CC ≤ 10, 400 líneas/archivo, 120 columnas, sin números/textos mágicos)
- `UserService` se divide para no superar 400 líneas ni CC: `AuthService` (login/refresh/logout), `UserService` (registro, perfil, admin), `RefreshTokenService`, `AdminBootstrap`.
- Constantes/propiedades en `application.yaml` con `${VAR:default}` y `@ConfigurationProperties` (`JwtProperties`, `BootstrapAdminProperties` sin default para secretos): TTL access/refresh, costo BCrypt, bytes del refresh (32), longitud mínima del secret (32), límites de contraseña. Textos de error repetidos como constantes.
- Lombok solo `@RequiredArgsConstructor` en `service/` y `controller/` y en entidades, sin `@Data` ni `@Autowired` (hoy `TokenService` usa `@Autowired` y constructores manuales → se corrige). Sin `@SuppressWarnings`/`NOSONAR`. Revisión final con la skill `quality-gates-java` (Checkstyle, PMD+CPD, SpotBugs/FindSecBugs, Dependency-Check, JaCoCo).
- Logs: solo IDs; RNF-02 (creación, login ok/fallo, desactivación) sin emails ni tokens.

## 6. Riesgos
1. **Cambio amplio de contrato y BD**: renombrar columnas/DTOs y eliminar `user_identifier` rompe a cualquier consumidor actual (contrato `ms-users.yaml`, ms-catalog/ms-tickets). Mitigación: documentos y contrato se actualizan primero; la migración V2 conserva datos (`full_name` concatenado).
2. Requerimientos de P1 (HU, GHERKIN) se modifican de forma deliberada: debe quedar trazado en la bitácora y en el PR ("qué cambié yo").
3. Tamaño del cambio (~20 archivos nuevos/modificados): se hace en commits por paso y con `verify` tras cada fase.
4. Clave HS256 compartida (D5, aceptado); secret < 32 bytes impide el arranque.
5. Access token vigente hasta 15 min tras desactivar (trade-off aceptado; el refresh sí se revoca).
6. `ADMIN_EMAIL`/`ADMIN_PASSWORD` sin default: la app y `MsUsersApplicationTests` requieren valores en test.
7. Testcontainers requiere Docker; sin Docker el `verify` queda "pendiente" (regla 30).
8. Dependency-Check requiere `NVD_API_KEY` y red.
9. Sin rate limiting ni CORS (D11, §6.2): documentar en SECURITY-REPORT.
10. Boot 4.1.2: paquetes/starters (Jackson 3, `webmvc-test`); compilar temprano.

## 7. Tareas
0. Actualizar `docs/requerimientos.md`, backlog, contrato OpenAPI y `docs/seguridad.md` según §5b-A (tu revisión antes de seguir). 1. pom (con tu confirmación). 2. Migraciones V2/V3, entidad, repos, DTOs y mapper (§5b-B). 3. Properties + Encoder/Decoder. 4. Converter, handlers, `SecurityConfig`. 5. `TokenService` Nimbus + controller/service sin parseo manual; división de servicios (§5b-C). 6. Orden de validación de login (401 genérico). 7. Refresh/logout. 8. `POST/GET /users` + `AdminBootstrap`. 9. Config/plantillas/Actuator. 10. Tests (seguridad + Testcontainers). 11. `verify`, agente revisor, SECURITY-REPORT. 12. Sobrescribir `ms-catalog/docs/planes/PLAN-SEGURIDAD-MS-USERS-P3.md` con este plan.