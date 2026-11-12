# P3 · Plan de seguridad para ms-users (Fernando)

> Estado: **Ejecutado hasta P3** (código alineado y pruebas de seguridad en verde). Pendientes: P4, P5 y P6. Este documento se actualizó tras las decisiones de la opción A; las divergencias de §1 están cerradas.
> Fuentes: `ARCHITECTURE.md` §3.1, §5.2, §6.1–§6.4; `docs/seguridad.md` (ajustado a tus decisiones); `docs/p3-prompt-maestro.md` P1.

## 0. Decisiones (ARCHITECTURE §3.1 es la fuente; `docs/seguridad.md` y el contrato ya están alineados)

| Tema | Decisión | Estado en el código | Acción |
|---|---|---|---|
| Login/refresh fallidos y cuenta inactiva | **401** `INVALID_CREDENTIALS` siempre, sin revelar estado | ✔ Cerrado: cuenta inactiva da 401 genérico (`UserService`, `AuthControllerSecurityTest`) | Ninguna |
| Bloqueo temporal (423) | **Eliminado**: no está en ARCHITECTURE ni en `seguridad.md` ajustado | ✔ Cerrado: no quedan `AccountLockedException` ni `lock.*`. V4 no borra columnas (ver §3 y §7.1) | Ninguna |
| Identificador en rutas | **`{id}` numérico** (`long`) | ✔ Cerrado: `/users/{id}` y `/users/{id}/status` con `Long` | Ninguna |
| Forma de DTOs | **La de ARCHITECTURE §3.1** | ✔ Cerrado: `UserResponse`, `TokenResponse` y `CreateUserRequest` alineados | Ninguna |
| `PUT /api/v1/users/me` | **Quitado** (ARCHITECTURE solo lista `GET`) | ✔ Cerrado: ya no está mapeado (`putMeIsNoLongerMappedEvenWithValidToken`) | Ninguna |
| Base de datos | PostgreSQL 16 + Testcontainers | Ya configurado; no hay H2 | Sin cambio |
| Pruebas JWT | `jwt()` de `spring-security-test`; token real firmado solo para 401 por firma/expiración | ✔ Cubierto en `UserControllerSecurityTest` (firma, expiración, `iss`, `aud`) | Ver §5 |

## 1. Estado de partida del código frente al objetivo (antes de P2; las divergencias están cerradas)

El código ya tiene la base de P2 (JWT, refresh, logout, bootstrap, handlers). Lo que falta es alinearlo con ARCHITECTURE:

- Ya existen: `TokenService`, `RefreshTokenService`, `V2__crear_tabla_refresh_tokens.sql`, `JwtEncoderConfig`, `JwtDecoderConfig`, `JwtRolesConverter`, `ProblemAuthenticationEntryPoint`, `ProblemAccessDeniedHandler`, `UnmappedRouteAuthorizationManager`, `AdminBootstrap`, y los tests `AuthControllerSecurityTest`, `UserControllerSecurityTest`, `UnmappedRouteSecurityTest`, `RefreshTokenServiceTest`, `TokenServiceTest`, `UserServiceTest`, `AdminBootstrapTest`.
- `SecurityConfig`: ya no tiene `throws Exception` en la firma (búsqueda sin coincidencias). El hallazgo de PMD de P0 debe confirmarse con `verify`.
- `UserController`: `@PreAuthorize("hasRole('ADMIN')")` en `GET /users`, `POST /users`, `GET /{userIdentifier}` y `PATCH /{userIdentifier}/status`. `GET /me` y `PUT /me` no tienen restricción de rol.
- `V1__crear_tablas_usuarios.sql` (commit `2ba348f`) define `failed_attempts`, `locked_until` y `refresh_token_hash`. No se edita: el bloqueo se retira con una migración nueva.
- `V3__ajustar_tipo_token_hash.sql` (sin commit) cambia `token_hash` a `VARCHAR(64)`. Es coherente con `RefreshToken.java:22` y con `V2` (`CHAR(64)`).

Divergencias con ARCHITECTURE §3.1 (todas en código; todas cerradas en los commits `2af0d44`…`5093f73`):

| # | Código antes | ARCHITECTURE §3.1 | Archivo | Estado |
|---|---|---|---|---|
| D-a | Cuenta inactiva en login: 403 `ACCOUNT_INACTIVE` | 401 `INVALID_CREDENTIALS` | `UserService.java:85`, `GlobalExceptionHandler.java:64` | ✔ Cerrado |
| D-b | Bloqueo: 423 `ACCOUNT_LOCKED` (`validateNotLocked`, `lock.*`) | No existe | `UserService.java:170–186`, `GlobalExceptionHandler.java:74` | ✔ Cerrado |
| D-c | Rutas `{userIdentifier}` (String) | `{id}` (long) | `UserController.java:61,67` | ✔ Cerrado |
| D-d | `PUT /api/v1/users/me` | No listado | `UserController.java:42` | ✔ Cerrado (quitado) |
| D-e | `UserResponse`: `userIdentifier`, `name`, `paternalName`, `maternalName`, `isActive` | `id`, `email`, `fullName`, `role`, `active`, `createdAt` | `dto/response/UserResponse.java` | ✔ Cerrado |
| D-f | `TokenResponse`: `expiresInSeconds`, sin `refreshExpiresIn` ni `tokenType` fijo | `expiresIn` (900), `refreshExpiresIn` (604800), `tokenType` `Bearer` | `dto/response/TokenResponse.java` | ✔ Cerrado |
| D-g | `CreateUserRequest` anida `user` (`RegisterUserRequest`) + `role` | Plano: `RegisterRequest` + `role` | `dto/request/CreateUserRequest.java` | ✔ Cerrado |

Nota: `RegisterUserRequest` sigue existiendo para `POST /auth/register` (público, sin `role`); `CreateUserRequest` es el plano del admin.


## 2. Dependencias

Revisado en `ms-users/pom.xml`:

| Artefacto | Estado | Motivo |
|---|---|---|
| `spring-boot-starter-security-oauth2-resource-server` | Presente | Resource server y `JwtDecoder` (nombre de Boot 4) |
| `spring-boot-starter-security-test` | Presente | `jwt()`, `@WithMockUser` |
| `spring-security-crypto` | Presente | BCrypt |
| `org.testcontainers:testcontainers-postgresql`, `testcontainers-junit-jupiter`, `spring-boot-testcontainers` | Presentes (test) | `MsUsersApplicationTests` ya usa `postgres:16` con `@ServiceConnection` |
| H2 | **No está** en el `pom.xml` | Nada que quitar |

Cambio aprobado por el usuario (opción A, commit `72ba20d`): se añade `spring-boot-flyway`, `flyway-core` y `flyway-database-postgresql` (migraciones con Flyway en Boot 4). Las versiones las gestiona el BOM de `spring-boot-starter-parent` 4.1.2. Cualquier dependencia nueva requiere confirmación previa (regla 2 del repositorio).

Docker está activo (`29.9.2`), así que las pruebas con Testcontainers se pueden ejecutar localmente.

## 3. Clases nuevas y modificadas (§5.2)

Nuevas:
- `db/migration/V4__alinear_usuarios_con_arquitectura.sql` (✔ hecha, no destructiva): añade `full_name` y lo rellena desde nombre y apellidos; amplía `email` a 254; relaja `NOT NULL` de `name`, `paternal_name` y `user_identifier`. **No elimina** `failed_attempts` ni `locked_until`: se conservan para no perder datos y V1 no se edita. Ninguna entidad las mapea, así que `ddl-auto=validate` no falla.

Modificadas:
- `application.yaml`: ✔ `server.error.include-stacktrace`, `include-message` y `include-binding-errors` en `never`; ✔ `management` solo expone `health` con `show-details=never`; ✔ bloque `app.security.lock.*` retirado. Los TTL van como `access-ttl-minutes: ${JWT_ACCESS_TTL_MINUTES:15}` y `refresh-ttl-days: ${JWT_REFRESH_TTL_DAYS:7}`, que equivalen a 900 s y 604800 s. Los secretos siguen como `${VAR}` sin valor por defecto.
- `config/SecurityConfig`: revisar que `/api/v1/**` sea la única excepción de CSRF y que el `try/catch` alrededor de `build()` no oculte fallos de configuración.
- `controller/UserController`: ✔ `{userIdentifier}` → `{id}` (`Long`); ✔ `GET /users/{id}` y `PATCH /users/{id}/status` con `@PreAuthorize("hasRole('ADMIN')")`; ✔ `PUT /me` retirado (§7.2).
- `controller/AuthController`: ✔ login y refresh fallidos → 401 `INVALID_CREDENTIALS`; ✔ `logout` exige token.
- `service/UserService`: ✔ login con cuenta inactiva → 401 genérico; ✔ `validateNotLocked`, `lockDurationMinutes` y `maxFailedAttempts` retirados.
- `exception/GlobalExceptionHandler`: ✔ handlers de 423 y de 403 por cuenta inactiva eliminados; el login usa 401 `INVALID_CREDENTIALS`. Los `type` usan `https://mesa-ayuda.ids/problems/<slug>` (commit `57bba7a`).
- `exception/AccountLockedException`, `exception/AccountInactiveException`: ✔ eliminadas (no quedan referencias en `ms-users/src`).
- `domain/User`: ✔ `failedAttempts` y `lockedUntil` no están mapeados; las columnas quedan en BD (V4 no las borra).
- `dto/response/UserResponse`: ✔ `id`, `email`, `fullName`, `role`, `active`, `createdAt` (D-e).
- `dto/response/TokenResponse`: ✔ `accessToken`, `tokenType` (`Bearer`), `expiresIn` (900), `refreshToken`, `refreshExpiresIn` (604800) (D-f).
- `dto/request/CreateUserRequest`: ✔ plano, con los campos de registro y `role` (D-g). `RegisterUserRequest` se mantiene para `POST /auth/register`.
- `docs/contratos/ms-users.yaml`: ya alineado en P0; se revisa en P6 si cambia algún DTO.

## 4. Matriz endpoint × rol (ARCHITECTURE §6.3)

| Endpoint | SOLICITANTE | AGENTE | ADMIN | Sin token | Prueba que lo cubre |
|---|---|---|---|---|---|
| `GET /api/v1/users/me` | ✔ 200 | ✔ 200 | ✔ 200 | 401 | `UserControllerSecurityTest` (`jwt()`) |
| `POST /api/v1/auth/logout` | ✔ 204 | ✔ 204 | ✔ 204 | 401 | `AuthControllerSecurityTest` |
| `GET /api/v1/users` | 403 | 403 | ✔ 200 | 401 | `UserControllerSecurityTest` |
| `GET /api/v1/users/{id}` | 403 | 403 | ✔ 200 | 401 | `UserControllerSecurityTest` |
| `POST /api/v1/users` | 403 | 403 | ✔ 201 | 401 | `UserControllerSecurityTest` |
| `PATCH /api/v1/users/{id}/status` | 403 | 403 | ✔ 200 | 401 | `UserControllerSecurityTest` |
| `POST /api/v1/auth/register` | público | público | público | público | `AuthControllerSecurityTest` |
| `POST /api/v1/auth/login` | público | público | público | público | `AuthControllerSecurityTest` (401 con credenciales malas y con cuenta inactiva) |
| `POST /api/v1/auth/refresh` | público | público | público | público | `AuthControllerSecurityTest` (token inválido → 401; reuso → revoca todo, probado en `RefreshTokenServiceTest`) |

Pruebas de 401 con token real firmado mal o expirado: `UserControllerSecurityTest` cubre firma alterada, token expirado, `iss` incorrecto y `aud` incorrecto. Responden 401 `UNAUTHORIZED` en formato ProblemDetail (ARCHITECTURE §6.2).

Recurso ajeno: `/users/{id}` es solo ADMIN, así que un SOLICITANTE o AGENTE recibe 403 (§7.4). La celda 403 de la matriz lo cubre.


## 5. Pruebas que demostrarán 401 / 403 / éxito

- **401**: sin token; firma alterada; token expirado; `iss` incorrecto; `aud` incorrecto; login con contraseña mala; login con cuenta inactiva (mismo 401 genérico).
- **403**: SOLICITANTE y AGENTE sobre cada endpoint de admin; SOLICITANTE sobre `/users/{id}` ajeno (si aplica).
- **Éxito**: cada celda con ✔ de la matriz.
- **Refresh**: rotación; reuso del token anterior → 401 y revoca todos los tokens del usuario (probado en `RefreshTokenServiceTest` a nivel servicio; a nivel HTTP solo se asserta el 401 de token inválido); `logout` devuelve 204 (la revocación del token en HTTP no tiene prueba propia todavía).
- **Bootstrap**: `AdminBootstrapTest` cubre crear y omitir. Que la contraseña no aparezca en logs no tiene prueba automática todavía.
- **Contexto**: `MsUsersApplicationTests` con Testcontainers `postgres:16` (requiere Docker).

Regla 17: cada regla de negocio nueva tiene una prueba positiva y una negativa.
Regla 20: si una prueba falla, se corrige el código de producción, no la prueba.

## 6. Riesgos

1. **Migración V1 aplicada**: no se edita. V4 es no destructiva: conserva `failed_attempts` y `locked_until` en BD y solo deja de mapearlos en JPA. Eliminarlas sería una V5 aparte si el usuario lo pide.
2. **Cambio de forma de la API**: `UserResponse`, `TokenResponse`, `CreateUserRequest` y las rutas `{id}` cambian. Quien consuma `ms-users` debe enterarse; el contrato ya refleja ARCHITECTURE.
3. **Hallazgo PMD**: el `throws` que lo causaba ya no está en `SecurityConfig`. Confirmado: `./mvnw -B verify` en BUILD SUCCESS (66 pruebas, 0 fallos).
4. **Access token de 15 min tras desactivar usuario**: trade-off aceptado en ARCHITECTURE §6.1.
5. **Rate limiting en login**: fuera de alcance (D11). Riesgo documentado.
6. **Commits**: el trabajo está en commits locales de `practica-3-fernando` (`2af0d44`…`57bba7a`), sin push. El `.gitignore` modificado no es parte de este trabajo.
7. **Cambios de docs del usuario**: `docs/seguridad.md` y el borrado de la etiqueta `p3-entrega-fernando` se hicieron por instrucción explícita, aunque `docs/p3-prompt-maestro.md` lo prohíbe.
8. **Códigos 415 y otras excepciones de MVC**: caen en 500 `INTERNAL_ERROR`. Fuera de alcance de P3; queda como deuda.

## 7. Decisiones confirmadas por el usuario

1. **Bloqueo 423: retirado** (alinear con ARCHITECTURE). ✔ Se quitó del código `failed_attempts`/`locked_until` del mapeo JPA, `AccountLockedException` y los `lock.*` de `application.yaml`. ✔ V4 es no destructiva: las columnas se conservan en BD (corrige la versión anterior de este punto, que decía que V4 las eliminaba).
2. **`PUT /api/v1/users/me`: quitado.** ✔ ARCHITECTURE solo lista `GET`.
3. **DTOs y rutas: alineados con ARCHITECTURE §3.1** (`{id}`, `UserResponse`, `TokenResponse`, `CreateUserRequest` plano). ✔ Hecho.
4. **Recurso ajeno: 403** para no-admin en `/users/{id}`, según ARCHITECTURE. ✔ Hecho.

### 7.5 Decisiones de la opción A (aprobadas en la revisión de commits)

- Código de 404 de ruta inexistente: `ROUTE_NOT_FOUND`.
- 405 sin campo `code`, con cabecera `Allow` conservada.
- Base de `type`: `https://mesa-ayuda.ids/problems/<slug>` (no `ids.com/errors`).
- Commit `89db372` reescrito a `72ba20d` (solo mensaje). Commits de código: `5093f73` (códigos de error), `57bba7a` (URIs de tipo).
- `pom.xml` (Flyway y Testcontainers, H2 retirado) y borrado de `ms-users/AGENTS.md`: incluidos en `72ba20d`, confirmados por el usuario.


## 8. Orden de ejecución

1. ✔ **P2A** (modelo, V4 no destructiva, DTOs, handlers y `UserService` para 401 en login).
2. ✔ **P2B** (`UserController`: `{id}`, `PUT /me` retirado; `application.yaml`; `verify` en BUILD SUCCESS).
3. ✔ **P3** (pruebas de seguridad según §4 y §5; 66 pruebas en verde). Quedan los huecos de §5 marcados arriba.
4. **P4** (agente revisor en `.github/agents/revisor-seguridad-ms-users.agent.md`): pendiente.
5. **P5** (integración: ms-users solo provee contrato; se documenta la excepción D13): pendiente.
6. **P6** (`SECURITY-REPORT`, alineación del contrato, `BITACORA-IA.md` sin sección P3, `verify` final, sugerencias de PR y tag en español): pendiente.

Pendiente de decisión del usuario: `docs/requerimientos.md` (líneas 119–124) y `docs/backlog.md:68` todavía piden 423 en HU-USR-02, y `plan-ms-users.md` líneas 34 y 75 lo mencionan. No se tocan sin confirmación.

Estado: P2 y P3 ejecutados y commiteados. Este documento se actualizó para reflejar el código real; los commits de este cambio de documento aún no se han hecho.


