# Plan de seguridad P3 — ms-users (Fernando)

Estado: **P1 (plan), pendiente de aprobación.** No se ha escrito código.
Fuente de verdad: `ARCHITECTURE.md` §0, §3.1, §5.2, §6 y §7 (decisión del usuario: `docs/seguridad.md` está vacío y no se modifica).

## 0. Hallazgos de P0 que afectan al plan

| # | Hallazgo | Acción propuesta |
|---|---|---|
| 1 | `docs/seguridad.md` vacío (0 bytes) | Se usa ARCHITECTURE §6 como fuente. No se escribe `seguridad.md`. |
| 2 | `docs/contratos/ms-users.yaml` no tiene `/auth/refresh`, `/auth/logout`, `GET/POST /users` ni `/users/{id}`; tiene `/users/{userIdentifier}/status` y `/users/me` | Actualizar el contrato en P6 para que coincida con ARCHITECTURE §3.1. |
| 3 | `pom.xml` usa `jjwt-api/impl/jackson 0.12.6` y no tiene ningún starter de Spring Security | Retirar jjwt y agregar starters de seguridad (ver §1). |
| 4 | Árbol con cambios fuera de alcance (`ms-users/ARCHITECTURE.bak`, `ms-catalog/prompts/p3-prompt-maestro.md`, `tabla-calidad-microservicios.md`) | No se incluyen en los commits de P3. |

## 1. Dependencias (verificadas en Maven Central, Boot 4.1.2)

| Agregar | Motivo | Verificación |
|---|---|---|
| `spring-boot-starter-security-oauth2-resource-server` | Validación del JWT (`NimbusJwtDecoder`) y, transitivamente, `spring-security-oauth2-jose` con `NimbusJwtEncoder` para emitir | Existe en `repo1.maven.org/.../4.1.2/` (nombre Boot 4, no el deprecado `spring-boot-starter-oauth2-resource-server`) |
| `spring-boot-starter-security-test` | `jwt()` y `SecurityMockMvcRequestPostProcessors` en pruebas | Existe en `repo1.maven.org/.../4.1.2/` |

Retirar: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, propiedad `jjwt.version`.
Mantener: `spring-security-crypto` (BCrypt), `spring-boot-starter-webmvc-test`.
Pendiente de confirmar al implementar: que `spring-boot-starter-security` (incluido transitivamente) no rompa las pruebas actuales de `@WebMvcTest`.

Java 17 y Boot 4.1.2 se mantienen. Jackson 3 (`tools.jackson`) se usa solo si hace falta tocar serialización directa.

## 2. Clases nuevas y modificadas (§5.2 de ARCHITECTURE)

**Nuevas**

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `SecurityConfig` | `config` | `SecurityFilterChain` común (§6.2): stateless, CSRF off, `oauth2ResourceServer().jwt()`, `@EnableMethodSecurity`, públicos `/auth/register`, `/auth/login`, `/auth/refresh`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**` |
| `JwtDecoderConfig` | `config` | `NimbusJwtDecoder.withSecretKey`; validadores de `iss=ms-users` y `aud=mesa-ayuda`; falla al arrancar si `JWT_SECRET` < 32 bytes |
| `JwtEncoderConfig` | `config` | `NimbusJwtEncoder` con `MacAlgorithm.HS256` (solo ms-users emite) |
| `JwtRolesConverter` | `security` | Lee el claim `roles` → `ROLE_<valor>` |
| `JwtProperties` | `config` | `record @ConfigurationProperties("app.security.jwt")`: `access-ttl` (15 min), `refresh-ttl` (7 días), `issuer`, `audience` |
| `ProblemAuthenticationEntryPoint` | `security` | 401 en `ProblemDetail` (`UNAUTHORIZED`) |
| `ProblemAccessDeniedHandler` | `security` | 403 en `ProblemDetail` (`FORBIDDEN`) |
| `PasswordEncoderConfig` | `config` | `BCryptPasswordEncoder` con costo configurable (por defecto 10) |
| `TokenService` | `service` | Emite access token (claims §6.1: `iss`, `aud`, `sub`=id, `roles`, `email`, `name`, `iat`, `exp`, `jti`) |
| `RefreshTokenService` | `service` | Genera token opaco (32 bytes `SecureRandom`, Base64URL), guarda SHA-256, rota en cada uso, detecta reutilización (revoca todos), revoca en logout y al desactivar |
| `RefreshToken` (entidad) | `model` | Tabla `refresh_tokens` |
| `RefreshTokenRepository` | `repository` | Lookup por `token_hash`, borrado/revocación por `user_id` |
| `AdminBootstrap` | `bootstrap` | Crea el ADMIN desde `ADMIN_EMAIL` / `ADMIN_PASSWORD` si no existe |
| `TokenResponse` | `dto.response` | `accessToken`, `tokenType`, `expiresIn` (900), `refreshToken`, `refreshExpiresIn` (604800) |
| `LogoutRequest`, `RefreshRequest` | `dto.request` | `refreshToken` |
| `CreateUserRequest`, `UpdateStatusRequest` | `dto.request` | §3.1 (`role` en el alta, `active` en el PATCH) |
| Migración `V2__create_refresh_tokens.sql` | `db/migration` | Tabla `refresh_tokens` con FK `ON DELETE CASCADE`, `UK(token_hash)`, `idx_refresh_tokens_user_id` (§2.1) |

**Modificadas**

| Clase / archivo | Cambio |
|---|---|
| `AuthController` | `POST /auth/login` → `TokenResponse`; `POST /auth/refresh`; `POST /auth/logout` (204) |
| `UserController` | `GET /users/me`; `GET /users?role=` (ADMIN); `GET /users/{id}` (ADMIN); `POST /users` (ADMIN, 201); `PATCH /users/{id}/status` (reemplaza `{userIdentifier}`) |
| `AuthService` / `UserService` | Login y refresh fallidos siempre 401 "Credenciales inválidas"; al desactivar revoca tokens |
| Entidad `User` | Sin cambios de dominio; se revisa que `email` se guarde en minúsculas |
| `application.yaml` | `${ENV:default}`; `JWT_SECRET` sin valor por defecto; Actuator solo `health`, `show-details=never`; `server.error.include-message/stacktrace=never` |
| Excepciones | Mensajes y `detail` en español; sin cambios de códigos |

Se retira `userIdentifier` (claim de jjwt) y el uso de `{userIdentifier}` en rutas; `sub` = id numérico.

Lo que **no** se toca en ms-users: no hay `client/` (D13), no hay rate limiting (D11), no hay CORS abierto.

## 3. Matriz endpoint × rol (ARCHITECTURE §6.3, filas de ms-users)

Columna "Sin token" = 401 para endpoints protegidos. Los públicos no piden token.

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN | Prueba que cubre cada celda |
|---|---|---|---|---|---|
| `POST /api/v1/auth/register` | público (201) | público | público | público | `AuthControllerTest` 201 y 400/409 sin token |
| `POST /api/v1/auth/login` | público (200 / 401 homogéneo) | público | público | público | Login OK, login fallido homogéneo, usuario inactivo → 401 igual |
| `POST /api/v1/auth/refresh` | público (200 / 401) | público | público | público | Rotación, reutilización → revoca todo, token expirado → 401 |
| `POST /api/v1/auth/logout` | 401 | ✔ 204 | ✔ 204 | ✔ 204 | 401 sin token; 204 con `jwt()` por rol |
| `GET /api/v1/users/me` | 401 | ✔ 200 | ✔ 200 | ✔ 200 | 401; 200 propio `sub` por rol |
| `GET /api/v1/users?role=AGENTE` | 401 | 403 | 403 | ✔ 200 | 401; 403 por rol; 200 ADMIN |
| `GET /api/v1/users/{id}` | 401 | 403 | 403 | ✔ 200 / 404 | 401; 403; 200; 404 |
| `POST /api/v1/users` | 401 | 403 | 403 | ✔ 201 / 409 | 401; 403; 201; 409 email duplicado |
| `PATCH /api/v1/users/{id}/status` | 401 | 403 | 403 | ✔ 200 / 404 | 401; 403; 200; 404 |

Un recurso ajeno devuelve 403 (aplica en ms-users solo a `/users/me`, que siempre es el propio usuario por `sub`).

## 4. Pruebas que demostrarán 401 / 403 / éxito (P3)

- **Por endpoint** con `@WebMvcTest` + `@Import(SecurityConfig.class)` y `jwt()`: 401 sin token, 403 con rol incorrecto, éxito con rol correcto.
- **401 adicionales**: firma alterada, token expirado, `iss` incorrecto, `aud` incorrecto. Respuesta ProblemDetail `UNAUTHORIZED`.
- **`JwtRolesConverter`**: un token con `roles: ["ADMIN"]` obtiene `ROLE_ADMIN`; sin el converter, la prueba de ADMIN fallaría con 403.
- **Actuator**: `/actuator/health` público; `/actuator/env`, `/actuator/beans` no expuestos (404 o 401).
- **BCrypt**: el hash almacenado no es la contraseña en claro y verifica con `matches`.
- **Refresh**: rotación (el token viejo ya no sirve); reutilización del token viejo revoca todos los del usuario; logout revoca.
- **Login fallido homogéneo**: email inexistente, contraseña mal y usuario inactivo devuelven el mismo 401 y mismo mensaje.
- **`AdminBootstrap`**: crea el ADMIN si no existe y no duplica en un segundo arranque.
- **Secreto**: `JWT_SECRET` de menos de 32 bytes impide arrancar. La prueba usa un secreto de prueba de ≥ 32 caracteres en `src/test/resources`.
- Las pruebas de P1/P2 se corrigen agregando `jwt()`. Nunca se desactiva la seguridad.

Verificación: `./mvnw -B verify` en verde sin PostgreSQL local (Testcontainers), JaCoCo ≥ 70 %, Checkstyle, PMD, SpotBugs y Dependency-Check sin cambios de umbral.

## 5. Riesgos (se documentan en SECURITY-REPORT en P6)

- Llave HS256 compartida entre los 3 servicios (D5, aceptado para la beca).
- Un access token sigue vigente hasta 15 min después de desactivar un usuario (trade-off aceptado en §6.1).
- Sin rate limiting en `/auth/login` (D11, fuera de alcance).
- BCrypt trunca a 72 bytes; la validación de contraseña limita a 72 caracteres.
- Dependencia de Boot 4.1.2 y Java 17: los starters y clases de Spring Security 7 deben confirmarse al implementar.

## 6. Decisiones que debe tomar el usuario (Fernando / equipo)

1. ¿Se actualiza `docs/contratos/ms-users.yaml` en P6 para que coincida con ARCHITECTURE §3.1? (recomendado: sí)
2. ¿Dónde se guarda el plan final? Este archivo está en `ms-users/docs/planes/`; el prompt también pide `ms-users/prompts/p3-plan.md` para el PR. ¿Se duplica o basta con uno?
3. ¿El ADMIN de `AdminBootstrap` se crea solo si no existe, o se rota la contraseña en cada arranque? (recomendado: solo si no existe)
4. ¿Se acepta el orden P2A / P2B del prompt (modelo, migración y emisión primero; resource server después)?
