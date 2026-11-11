# P3 · Plan de seguridad para ms-users (Fernando)

> Estado: **PLAN, sin código.** Pendiente de aprobación.
> Fuentes: `ARCHITECTURE.md` §3.1, §5.2, §6.1–§6.4; `docs/seguridad.md` (ajustado a tus decisiones); `docs/p3-prompt-maestro.md` P1.

## 0. Decisiones (ARCHITECTURE §3.1 es la fuente; `docs/seguridad.md` y el contrato ya están alineados)

| Tema | Decisión | Estado en el código | Acción |
|---|---|---|---|
| Login/refresh fallidos y cuenta inactiva | **401** `INVALID_CREDENTIALS` siempre, sin revelar estado | Cuenta inactiva da 403 `ACCOUNT_INACTIVE` | Cambiar a 401 en `UserService` y `GlobalExceptionHandler` |
| Bloqueo temporal (423) | **Eliminado**: no está en ARCHITECTURE ni en `seguridad.md` ajustado | Implementado (`failed_attempts`, `locked_until`, `AccountLockedException`) | Quitar código y añadir V4 (ver §3 y §7.1) |
| Identificador en rutas | **`{id}` numérico** (`long`) | `{userIdentifier}` (String) | Cambiar en `UserController` |
| Forma de DTOs | **La de ARCHITECTURE §3.1** | `UserResponse`, `TokenResponse` y `CreateUserRequest` difieren | Alinear (§7.3) |
| `PUT /api/v1/users/me` | **Pendiente** (ARCHITECTURE solo lista `GET`) | Existe | Decidir (§7.2) |
| Base de datos | PostgreSQL 16 + Testcontainers | Ya configurado; no hay H2 | Sin cambio |
| Pruebas JWT | `jwt()` de `spring-security-test`; token real firmado solo para 401 por firma/expiración | Por verificar | Ver §5 |

## 1. Estado actual del código frente al objetivo

El código ya tiene la base de P2 (JWT, refresh, logout, bootstrap, handlers). Lo que falta es alinearlo con ARCHITECTURE:

- Ya existen: `TokenService`, `RefreshTokenService`, `V2__crear_tabla_refresh_tokens.sql`, `JwtEncoderConfig`, `JwtDecoderConfig`, `JwtRolesConverter`, `ProblemAuthenticationEntryPoint`, `ProblemAccessDeniedHandler`, `UnmappedRouteAuthorizationManager`, `AdminBootstrap`, y los tests `AuthControllerSecurityTest`, `UserControllerSecurityTest`, `UnmappedRouteSecurityTest`, `RefreshTokenServiceTest`, `TokenServiceTest`, `UserServiceTest`, `AdminBootstrapTest`.
- `SecurityConfig`: ya no tiene `throws Exception` en la firma (búsqueda sin coincidencias). El hallazgo de PMD de P0 debe confirmarse con `verify`.
- `UserController`: `@PreAuthorize("hasRole('ADMIN')")` en `GET /users`, `POST /users`, `GET /{userIdentifier}` y `PATCH /{userIdentifier}/status`. `GET /me` y `PUT /me` no tienen restricción de rol.
- `V1__crear_tablas_usuarios.sql` (commit `2ba348f`) define `failed_attempts`, `locked_until` y `refresh_token_hash`. No se edita: el bloqueo se retira con una migración nueva.
- `V3__ajustar_tipo_token_hash.sql` (sin commit) cambia `token_hash` a `VARCHAR(64)`. Es coherente con `RefreshToken.java:22` y con `V2` (`CHAR(64)`).

Divergencias con ARCHITECTURE §3.1 (todas en código, ninguna en docs ya alineados):

| # | Código hoy | ARCHITECTURE §3.1 | Archivo |
|---|---|---|---|
| D-a | Cuenta inactiva en login: 403 `ACCOUNT_INACTIVE` | 401 `INVALID_CREDENTIALS` | `UserService.java:85`, `GlobalExceptionHandler.java:64` |
| D-b | Bloqueo: 423 `ACCOUNT_LOCKED` (`validateNotLocked`, `lock.*`) | No existe | `UserService.java:170–186`, `GlobalExceptionHandler.java:74` |
| D-c | Rutas `{userIdentifier}` (String) | `{id}` (long) | `UserController.java:61,67` |
| D-d | `PUT /api/v1/users/me` | No listado | `UserController.java:42` |
| D-e | `UserResponse`: `userIdentifier`, `name`, `paternalName`, `maternalName`, `isActive` | `id`, `email`, `fullName`, `role`, `active`, `createdAt` | `dto/response/UserResponse.java` |
| D-f | `TokenResponse`: `expiresInSeconds`, sin `refreshExpiresIn` ni `tokenType` fijo | `expiresIn` (900), `refreshExpiresIn` (604800), `tokenType` `Bearer` | `dto/response/TokenResponse.java` |
| D-g | `CreateUserRequest` anida `user` (`RegisterUserRequest`) + `role` | Plano: `RegisterRequest` + `role` | `dto/request/CreateUserRequest.java` |

## 2. Dependencias

Revisado en `ms-users/pom.xml`:

| Artefacto | Estado | Motivo |
|---|---|---|
| `spring-boot-starter-security-oauth2-resource-server` | Presente | Resource server y `JwtDecoder` (nombre de Boot 4) |
| `spring-boot-starter-security-test` | Presente | `jwt()`, `@WithMockUser` |
| `spring-security-crypto` | Presente | BCrypt |
| `org.testcontainers:testcontainers-postgresql`, `testcontainers-junit-jupiter`, `spring-boot-testcontainers` | Presentes (test) | `MsUsersApplicationTests` ya usa `postgres:16` con `@ServiceConnection` |
| H2 | **No está** en el `pom.xml` | Nada que quitar |

No hay cambios previstos en `pom.xml`. Las versiones las gestiona el BOM de `spring-boot-starter-parent` 4.1.2. Si algo se añade, requiere confirmación previa (regla 2 del repositorio).

Docker está activo (`29.9.2`), así que las pruebas con Testcontainers se pueden ejecutar localmente.

## 3. Clases nuevas y modificadas (§5.2)

Nuevas:
- `db/migration/V4__retirar_bloqueo_usuarios.sql`: elimina `failed_attempts` y `locked_until` de `users` (V1 no se edita). Solo si §7.1 confirma retirar el bloqueo.

Modificadas:
- `application.yaml`: `server.error.include-stacktrace=never` e `include-message=never`; `management.endpoints.web.exposure.include=health` con `show-details=never`; TTL de access (900 s) y refresh (604800 s) según ARCHITECTURE §6.1 como `${VAR:default}`; quitar el bloque `app.security.lock.*` si se retira el bloqueo. Los secretos siguen como `${VAR}` sin valor por defecto.
- `config/SecurityConfig`: revisar que `/api/v1/**` sea la única excepción de CSRF y que el `try/catch` alrededor de `build()` no oculte fallos de configuración.
- `controller/UserController`: `{userIdentifier}` → `{id}` (`Long`); `GET /users/{id}` y `PATCH /users/{id}/status` mantienen `@PreAuthorize("hasRole('ADMIN')")`; `PUT /me` según §7.2.
- `controller/AuthController`: login y refresh fallidos → 401 `INVALID_CREDENTIALS`; `logout` exige token.
- `service/UserService`: login con cuenta inactiva → 401 genérico; si se retira el bloqueo, quitar `validateNotLocked`, `lockDurationMinutes` y `maxFailedAttempts`.
- `exception/GlobalExceptionHandler`: eliminar los handlers de `AccountLockedException` (423) y de `AccountInactiveException` (403); el login usa 401 `INVALID_CREDENTIALS`.
- `exception/AccountLockedException`, `exception/AccountInactiveException`: se eliminan si ya no tienen uso.
- `domain/User`: quitar `failedAttempts` y `lockedUntil` si se retira el bloqueo.
- `dto/response/UserResponse`: `id`, `email`, `fullName`, `role`, `active`, `createdAt` (D-e); `mapper/UserMapper` se ajusta.
- `dto/response/TokenResponse`: `accessToken`, `tokenType` (`Bearer`), `expiresIn` (900), `refreshToken`, `refreshExpiresIn` (604800) (D-f).
- `dto/request/CreateUserRequest`: plano, con los campos de `RegisterRequest` y `role` (D-g).
- `docs/contratos/ms-users.yaml`: ya alineado en P0; se revisa en P6 si cambia algún DTO.

## 4. Matriz endpoint × rol (ARCHITECTURE §6.3)

| Endpoint | SOLICITANTE | AGENTE | ADMIN | Sin token | Prueba que lo cubre |
|---|---|---|---|---|---|
| `GET /api/v1/users/me` | ✔ 200 | ✔ 200 | ✔ 200 | 401 | `UserControllerSecurityTest` (`jwt()`) |
| `PUT /api/v1/users/me` | ✔ 200 | ✔ 200 | ✔ 200 | 401 | `UserControllerSecurityTest` (solo si §7.2 lo mantiene) |
| `POST /api/v1/auth/logout` | ✔ 204 | ✔ 204 | ✔ 204 | 401 | `AuthControllerSecurityTest` |
| `GET /api/v1/users` | 403 | 403 | ✔ 200 | 401 | `UserControllerSecurityTest` |
| `GET /api/v1/users/{id}` | 403 | 403 | ✔ 200 | 401 | `UserControllerSecurityTest` |
| `POST /api/v1/users` | 403 | 403 | ✔ 201 | 401 | `UserControllerSecurityTest` |
| `PATCH /api/v1/users/{id}/status` | 403 | 403 | ✔ 200 | 401 | `UserControllerSecurityTest` |
| `POST /api/v1/auth/register` | público | público | público | público | `AuthControllerSecurityTest` |
| `POST /api/v1/auth/login` | público | público | público | público | `AuthControllerSecurityTest` (401 con credenciales malas y con cuenta inactiva) |
| `POST /api/v1/auth/refresh` | público | público | público | público | `AuthControllerSecurityTest` (reuso → revoca todo) |

Pruebas de 401 con token real firmado mal o expirado: `jwt()` no sirve para esto. Se usa un `JwtEncoder` con otra clave o `exp` pasado, y se verifica 401 `UNAUTHORIZED` en formato ProblemDetail (ARCHITECTURE §6.2). Un token con `iss` o `aud` incorrectos también → 401.

Recurso ajeno: en ms-users, un SOLICITANTE o AGENTE no puede leer `/users/{id}` de otro usuario porque ese endpoint es solo ADMIN, y la respuesta es 403 (§7.4). La celda 403 de la matriz lo cubre.

## 5. Pruebas que demostrarán 401 / 403 / éxito

- **401**: sin token; firma alterada; token expirado; `iss` incorrecto; `aud` incorrecto; login con contraseña mala; login con cuenta inactiva (mismo 401 genérico).
- **403**: SOLICITANTE y AGENTE sobre cada endpoint de admin; SOLICITANTE sobre `/users/{id}` ajeno (si aplica).
- **Éxito**: cada celda con ✔ de la matriz.
- **Refresh**: rotación; reuso del token anterior → 401 y revoca todos los tokens del usuario; `logout` revoca el token.
- **Bootstrap**: `AdminBootstrap` crea el admin solo si no existe; sin contraseña en logs.
- **Contexto**: `MsUsersApplicationTests` con Testcontainers `postgres:16` (requiere Docker).

Regla 17: cada regla de negocio nueva tiene una prueba positiva y una negativa.
Regla 20: si una prueba falla, se corrige el código de producción, no la prueba.

## 6. Riesgos

1. **Migración V1 aplicada**: no se edita. Si se retira el bloqueo, la V4 elimina `failed_attempts` y `locked_until`. Si V1 ya se aplicó en alguna base, V4 es la única vía.
2. **Cambio de forma de la API**: `UserResponse`, `TokenResponse`, `CreateUserRequest` y las rutas `{id}` cambian. Quien consuma `ms-users` debe enterarse; el contrato ya refleja ARCHITECTURE.
3. **Hallazgo PMD**: P0 reportó fallo de PMD y `BUILD SUCCESS` con 49 pruebas. El `throws` que lo causaba ya no está en `SecurityConfig`. Se confirma con `verify` en P2.
4. **Access token de 15 min tras desactivar usuario**: trade-off aceptado en ARCHITECTURE §6.1.
5. **Rate limiting en login**: fuera de alcance (D11). Riesgo documentado.
6. **Cambios sin commit**: el trabajo parte del árbol actual (decisión 4 del usuario). El `.gitignore` modificado no es parte de este trabajo.
7. **Cambios de docs del usuario**: `docs/seguridad.md` y el borrado de la etiqueta `p3-entrega-fernando` se hicieron por instrucción explícita, aunque `docs/p3-prompt-maestro.md` lo prohíbe.

## 7. Decisiones confirmadas por el usuario

1. **Bloqueo 423: retirar** (alinear con ARCHITECTURE). Se quita `failed_attempts`, `locked_until`, `AccountLockedException` y los `lock.*` de `application.yaml`, y se añade V4.
2. **`PUT /api/v1/users/me`: quitar.** ARCHITECTURE solo lista `GET`.
3. **DTOs y rutas: alinear con ARCHITECTURE §3.1** (`{id}`, `UserResponse`, `TokenResponse`, `CreateUserRequest` plano).
4. **Recurso ajeno: 403** para no-admin en `/users/{id}`, según ARCHITECTURE.


## 8. Orden de ejecución propuesto

1. **P2A** (modelo, V4 si §7.1, DTOs según §7.3, handlers y `UserService` para 401 en login): sin tocar la cadena de seguridad.
2. **P2B** (`UserController`: `{id}`, `PUT /me` según §7.2; `SecurityConfig`; `application.yaml`): verificar con `./mvnw -B verify`.
3. **P3** (pruebas de seguridad según §4 y §5).
4. **P4** (agente revisor en `.github/agents/revisor-seguridad-ms-users.agent.md`).
5. **P5** (integración: ms-users solo provee contrato; se documenta la excepción D13).
6. **P6** (`SECURITY-REPORT`, alineación del contrato, `verify` final, sugerencias de PR y tag en español).

Espero tu aprobación antes de tocar código.

