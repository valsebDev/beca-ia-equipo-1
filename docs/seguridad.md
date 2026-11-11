# Seguridad del sistema — Mesa de Ayuda

Acuerdo del equipo para la seguridad de los tres microservicios (`ms-users`, `ms-catalog`, `ms-tickets`). Es la referencia que consumen los agentes (P2–P4) y los revisores del PR.

Fuentes: [ARCHITECTURE.md](../ARCHITECTURE.md) §0, §3.4, §6 y §7 (fuente de verdad) y los planes [plan-ms-users](planes/plan-ms-users.md), [plan-ms-catalog](planes/plan-ms-catalog.md) y [plan-ms-tickets](planes/plan-ms-tickets.md). Donde un plan y ARCHITECTURE difieren, prevalece ARCHITECTURE; las diferencias abiertas están en §10.

## 1. Decisiones

| # | Tema | Decisión |
|---|------|----------|
| D1 | Identidad | JWT firmado con HS256. Se emite solo en `ms-users` y se valida en los tres servicios. |
| D2 | Librería JWT | Nimbus (`NimbusJwtEncoder` / `NimbusJwtDecoder`) con `spring-boot-starter-security-oauth2-resource-server`. Se retira JJWT de `ms-users`. |
| D3 | Identificador de usuario | `sub` = id numérico (`Long`, como texto). Rutas con `{id}`. |
| D4 | Login con cuenta inactiva | **401** `INVALID_CREDENTIALS` genérico; no revela el estado de la cuenta. |
| D5 | Cuenta bloqueada | Descartado: ARCHITECTURE no define bloqueo por intentos fallidos. Login fallido responde 401 (§4). |
| D6 | Vigencia | Access token 15 min (`expiresIn` = 900). Refresh token 7 días (`refreshExpiresIn` = 604800). |
| D7 | Endpoints adicionales de ms-users | `POST /auth/refresh`, `POST /auth/logout`, `GET /users`, `POST /users`. |
| D8 | Base de datos en pruebas | PostgreSQL 16 con Testcontainers (`@ServiceConnection`). Sin H2. |
| D9 | Administrador inicial | `AdminBootstrap` crea un ADMIN desde `ADMIN_EMAIL` / `ADMIN_PASSWORD` solo si no existe ninguno. |
| D10 | Reactivación de usuario | `PATCH /api/v1/users/{id}/status` con `{"active": true}`. |
| D11 | Fuera de alcance | Rate limiting, CORS abierto, Resilience4j, correlation-id, paginación. Riesgos aceptados. |
| D12 | Sin librería común | Cada servicio implementa su propia configuración de seguridad; no hay módulo compartido. |
| D13 | ADMIN activo en ms-catalog | En `POST/PUT/DELETE`, ms-catalog consulta a ms-users que el ADMIN autenticado siga activo. `ms-users` no consume ningún servicio. |

## 2. Identidad: JWT HS256

| Claim | Ejemplo | Uso |
|---|---|---|
| `iss` | `ms-users` | Validado por los tres servicios |
| `aud` | `mesa-ayuda` | Validado por los tres servicios |
| `sub` | `"42"` | Id del usuario: `requester_id`, `changed_by`, chequeo de propiedad |
| `roles` | `["AGENTE"]` | Se convierte en `ROLE_AGENTE` |
| `email`, `name` | — | Solo informativos; el email nunca se registra en logs |
| `iat`, `exp` | — | Access token de 15 min |
| `jti` | UUID | Trazabilidad |

- **Firma**: `JWT_SECRET` de al menos 32 bytes (UTF-8). Se lee solo de variable de entorno, sin valor por defecto. `JwtProperties` hace fallar el arranque si es más corta.
- **Validación** (los tres servicios): `NimbusJwtDecoder.withSecretKey` HS256 con validadores de `iss`, `aud` y expiración. Un token con firma alterada, expirado o con `iss`/`aud` incorrectos responde 401.
- **Roles**: `JwtRolesConverter` convierte solo el claim `roles` en `ROLE_<VALOR>`; tolera claim ausente o de tipo inválido, y un `scope` nunca otorga roles.
- **Roles válidos**: `SOLICITANTE`, `AGENTE`, `ADMIN`.

## 3. Reglas comunes a los tres servicios

1. `SecurityFilterChain` stateless, CSRF deshabilitado (API sin cookies), `oauth2ResourceServer().jwt()` y `@EnableMethodSecurity`.
2. Públicos: `/actuator/health`, `/v3/api-docs/**` y `/swagger-ui/**` (activables con `SWAGGER_ENABLED`, por defecto `true`; en producción, `false`). En `ms-users` además `POST /api/v1/auth/register`, `/login` y `/refresh`. Todo lo demás exige autenticación.
3. Actuator: `management.endpoints.web.exposure.include=health` y `show-details=never`.
4. 401 y 403 se devuelven como `ProblemDetail` (RFC 9457, `application/problem+json`) desde un `AuthenticationEntryPoint` y un `AccessDeniedHandler` propios. `AccessDeniedException` lanzada desde un controller responde 403, no 500.
5. `server.error.include-message=never` y `include-stacktrace=never`. Un error no controlado responde 500 `INTERNAL_ERROR` sin stack trace, clases ni mensajes internos; el detalle va solo al log del servidor.
6. Regla de rol: en el controller (`@PreAuthorize`) o por ruta en `SecurityConfig`, nunca en ambos con criterios distintos. Regla de propiedad y de estado: en el service con `CurrentUser` (id del `sub` y roles). Un recurso ajeno responde **403**, no 404.
7. CORS deshabilitado. Si se necesita, un `CorsConfigurationSource` con `CORS_ALLOWED_ORIGINS` y nunca `*`.
8. Todo DTO de entrada lleva Bean Validation con tamaño máximo en los textos, y no se expone la entidad (sin mass assignment).
9. Acceso a datos solo con Spring Data o `@Query` con parámetros nombrados (`:param`). Prohibido concatenar entrada del usuario en SQL o JPQL.
10. Logs: se registran solo IDs. Nunca contraseñas, tokens, el encabezado `Authorization` ni emails.
11. Los secretos se leen como `${VAR}` sin valor por defecto. Ningún `.yaml` ni código lleva contraseñas, claves o IPs fijas. El `.env` no se versiona.
12. Las pruebas de seguridad nunca desactivan los filtros (`addFilters=false` está prohibido).

## 4. Autenticación y sesión (ms-users)

- **Refresh token**: opaco, 32 bytes de `SecureRandom` en Base64URL. En la base solo se guarda su SHA-256 (`refresh_tokens.token_hash`).
- **Rotación**: cada refresh token se usa una vez; al usarlo se revoca y se emite otro.
- **Reutilización**: un refresh token ya revocado revoca todos los del usuario y responde 401.
- **Logout** (`POST /auth/logout`, autenticado): revoca el refresh token del body y responde 204.
- **Desactivar usuario**: revoca todos sus refresh tokens. Su access token sigue válido hasta 15 min (riesgo aceptado).
- **Contraseñas**: BCrypt con costo `BCRYPT_COST` (por defecto 10). Longitud 8 a 72 con al menos una letra y un dígito (BCrypt trunca a 72 bytes). Se guardan solo como `password_hash`.
- **Email**: se normaliza a minúsculas; único sin distinguir mayúsculas.
- **Administrador inicial**: `AdminBootstrap` con `ADMIN_EMAIL` / `ADMIN_PASSWORD` sin valor por defecto; no duplica si ya existe un ADMIN.

### Orden de validación en login (HU-USR-02 / HU-USR-03)

1. Buscar el usuario por email en minúsculas.
2. Contraseña incorrecta → **401** `INVALID_CREDENTIALS`.
3. Cuenta inactiva → **401** `INVALID_CREDENTIALS`.
4. Éxito: emitir tokens.

Login y refresh fallidos responden siempre 401 con el mismo mensaje, para no revelar si el email existe ni si la cuenta está inactiva.

## 5. Autorización: matrices endpoint × rol

Leyenda: ✔ permitido · 403 rol insuficiente o recurso ajeno · 401 sin token o token inválido/expirado · — público.

### 5.1 ms-users

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN | Éxito |
|---|---|---|---|---|---|
| `POST /api/v1/auth/register` | — | — | — | — | 201 (400, 409) |
| `POST /api/v1/auth/login` | — | — | — | — | 200 (401) |
| `POST /api/v1/auth/refresh` | — | — | — | — | 200 (401) |
| `POST /api/v1/auth/logout` | 401 | ✔ | ✔ | ✔ | 204 |
| `GET /api/v1/users/me` | 401 | ✔ | ✔ | ✔ | 200 |
| `GET /api/v1/users?role=` | 401 | 403 | 403 | ✔ | 200 |
| `GET /api/v1/users/{id}` | 401 | 403 | 403 | ✔ | 200 (404) |
| `POST /api/v1/users` | 401 | 403 | 403 | ✔ | 201 (400, 409) |
| `PATCH /api/v1/users/{id}/status` | 401 | 403 | 403 | ✔ | 200 (400, 404) |
| `GET /actuator/health` | ✔ | ✔ | ✔ | ✔ | 200 |
| Otro `/actuator/*` | 401 | 404 | 404 | 404 | — |

`/users/me` opera siempre sobre el `sub`; no existe recurso ajeno. Solo ADMIN puede consultar o cambiar el estado de otro usuario.

### 5.2 ms-catalog

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN |
|---|---|---|---|---|
| `GET /api/v1/categories`, `/{id}` | 401 | ✔ | ✔ | ✔ |
| `POST /api/v1/categories` | 401 | 403 | 403 | 201 |
| `PUT /api/v1/categories/{id}` | 401 | 403 | 403 | 200 |
| `DELETE /api/v1/categories/{id}` | 401 | 403 | 403 | 204 |
| `GET /api/v1/priorities`, `/{id}` | 401 | ✔ | ✔ | ✔ |
| `POST /api/v1/priorities` | 401 | 403 | 403 | 201 |
| `PUT /api/v1/priorities/{id}/sla` | 401 | 403 | 403 | 200 |
| `GET /actuator/health`, `/v3/api-docs`, `/swagger-ui` | ✔ | ✔ | ✔ | ✔ |
| Otras rutas de actuator | 401 | — | — | — |

Regla de rol con `@PreAuthorize("hasRole('ADMIN')")` en las mutaciones de `CategoryController` y `PriorityController`. En 403 el service no se invoca.

**ADMIN activo (D13)**: antes de `POST/PUT/DELETE`, `AdminGuard` consulta `GET /api/v1/users/{sub}` en ms-users con el token original del usuario.

| Respuesta de ms-users | Resultado en ms-catalog |
|---|---|
| 200 con `active=true` | La operación continúa |
| 404 o `active=false` | 403 `FORBIDDEN` |
| 401/403 del proveedor, 5xx, timeout o conexión rechazada | 503 `DEPENDENCY_UNAVAILABLE` |

Los `GET` de ms-catalog no llaman a ms-users y siguen funcionando si ms-users cae.

### 5.3 ms-tickets

Un recurso ajeno responde 403 (revela su existencia; lo exige la guía).

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN |
|---|---|---|---|---|
| `POST /api/v1/tickets` | 401 | 201 | 403 | 403 |
| `GET /api/v1/tickets` | 401 | 200 (propios) | 200 (asignados) | 200 (todos) |
| `GET /api/v1/tickets/{id}`, `/{id}/history` | 401 | 200 si es dueño, 403 si no | 200 si es asignado, 403 si no | 200 |
| `PATCH /{id}/assignment` | 401 | 403 | 403 | 200 (ABIERTO o ASIGNADO) |
| `PATCH /{id}/status` → EN_PROGRESO (desde ASIGNADO) | 401 | 403 | 200 si es asignado | 200 |
| `PATCH /{id}/status` → RESUELTO (desde EN_PROGRESO) | 401 | 403 | 200 si es asignado | 200 |
| `PATCH /{id}/status` → CERRADO (desde RESUELTO) | 401 | 200 si es dueño | 403 | 200 |
| `PATCH /{id}/status` → CANCELADO (desde ABIERTO/ASIGNADO) | 401 | 200 si es dueño | 403 | 200 |
| `GET /actuator/health` | ✔ | ✔ | ✔ | ✔ |

- La regla de rol por ruta vive en `SecurityConfig` (`hasRole`, con `@EnableMethodSecurity` activo). Propiedad (dueño, asignado) y transición de estado viven en `TicketAccessPolicy` / `TicketService` con `CurrentUser(sub, roles)` construido desde el JWT; los encabezados `X-User-Id` / `X-User-Role` dejan de usarse.
- `requireRole` en el service queda como defensa en profundidad.
- Al asignar, el asignado debe existir, estar activo y tener rol AGENTE en ms-users (422 `ASSIGNEE_INVALID` si no).

## 6. Propagación entre servicios

- **Sin credenciales de servicio**: `BearerTokenRelayInterceptor` (`ClientHttpRequestInterceptor` de `RestClient`) copia el `Authorization` del `SecurityContext`; la llamada se autoriza con los permisos del usuario original.
- Clientes `@HttpExchange` con `RestClient` (sin Feign ni Spring Cloud), `baseUrl` por `ClientsProperties` (`SERVICES_CATALOG_URL`, `SERVICES_USERS_URL`), timeouts connect 2 s / read 3 s, sin reintentos.
- DTOs de cliente propios de cada servicio, sin reutilizar clases del proveedor.
- Dependencias: ms-tickets → ms-catalog y ms-users; ms-catalog → ms-users (D13); ms-users no consume nada.

| Servicio | Respuesta del proveedor | Resultado |
|---|---|---|
| ms-tickets | 404 de categoría / prioridad / usuario | 422 `CATEGORY_INVALID` / `PRIORITY_INVALID` / `ASSIGNEE_INVALID` |
| ms-tickets | 401/403 del proveedor | 503 `DEPENDENCY_UNAVAILABLE` con log WARN (sin token ni email) |
| ms-tickets, ms-catalog | 5xx, timeout o conexión rechazada | 503 `DEPENDENCY_UNAVAILABLE` |

Si el interceptor no copia el token, el proveedor responde 401 y el consumidor lo mapea a 503.

## 7. Códigos de error (ARCHITECTURE §3.4)

Cada respuesta de error es un `ProblemDetail` con campo `code`; el `detail` va en español y las excepciones de dominio extienden `DomainException`.

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` / `MALFORMED_REQUEST` | Bean Validation / JSON ilegible |
| 401 | `UNAUTHORIZED` / `INVALID_CREDENTIALS` | Sin token o token inválido; login o refresh fallido |
| 403 | `FORBIDDEN` | Rol insuficiente, recurso ajeno o ADMIN inactivo (D13) |
| 404 | `USER_NOT_FOUND` y equivalentes | Recurso inexistente |
| 409 | `USER_ALREADY_EXISTS` | Email duplicado |
| 422 | `CATEGORY_INVALID` / `PRIORITY_INVALID` / `ASSIGNEE_INVALID` | Referencia inválida en ms-tickets |
| 500 | `INTERNAL_ERROR` | Genérico, sin detalle interno |
| 503 | `DEPENDENCY_UNAVAILABLE` | Servicio dependiente caído, lento o que rechaza el token |

`ms-users` renombra `UNAUTHENTICATED` y `FORBIDDEN_OPERATION` a `UNAUTHORIZED` y `FORBIDDEN`.

## 8. Configuración y secretos

| Variable | Servicios | Obligatoria | Por defecto | Uso |
|---|---|---|---|---|
| `JWT_SECRET` | los 3 | Sí | — | Clave HS256 (≥ 32 bytes), la misma en los tres |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | los 3 | Sí | — | Credenciales de su propia base |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | ms-users | Sí | — | ADMIN inicial |
| `SERVICES_USERS_URL` | ms-catalog, ms-tickets | Sí | — | URL de ms-users |
| `SERVICES_CATALOG_URL` | ms-tickets | Sí | — | URL de ms-catalog |
| `JWT_ACCESS_TTL_MINUTES` | ms-users | No | 15 | Vigencia del access token |
| `JWT_REFRESH_TTL_DAYS` | ms-users | No | 7 | Vigencia del refresh token |
| `BCRYPT_COST` | ms-users | No | 10 | Costo de BCrypt |
| `SWAGGER_ENABLED` | los 3 | No | `true` | Publica `/swagger-ui` y `/v3/api-docs`; `false` en producción |
| `CORS_ALLOWED_ORIGINS` | los 3 | No | vacío | Orígenes permitidos; nunca `*` |
| `NVD_API_KEY` | CI | Sí en CI | — | Dependency-Check (secreto de GitHub) |

Los valores de ejemplo van solo en `.env.example`. Los TTL, límites y URLs se leen con records `@ConfigurationProperties`, no como constantes en el código.

## 9. Pruebas que demuestran la seguridad

Cada regla nueva tiene al menos una prueba positiva y una negativa. Las pruebas de controller usan `@WebMvcTest` + `@Import(SecurityConfig)` y `jwt()` o tokens firmados reales; para simular beans, `@MockitoBean`.

- **401** (por servicio, `SecurityTokenValidationTest` con `JwtDecoder` real): sin token, firma alterada, expirado, `iss` incorrecto y `aud` incorrecto → 401 `UNAUTHORIZED` en ProblemDetail.
- **403 y éxito**: matriz parametrizada endpoint × {sin token, SOLICITANTE, AGENTE, ADMIN} en `UserControllerSecurityTest`, `AuthControllerSecurityTest`, `CategoryControllerSecurityTest`, `PriorityControllerSecurityTest` y las pruebas de `security/` de ms-tickets. En cada 403 se verifica `verifyNoInteractions(service)`.
- **Propiedad** (ms-tickets): recurso ajeno para SOLICITANTE y AGENTE → 403; dueño y asignado → éxito; AGENTE no dueño no cierra ni cancela.
- **Roles**: `JwtRolesConverterTest` (lista válida, vacía, ausente, tipo inválido, minúsculas; `scope` no otorga roles).
- **Configuración**: `JwtPropertiesTest` / arranque falla con `JWT_SECRET` de menos de 32 bytes; `ActuatorSecurityTest` en ms-users.
- **ms-users**: `TokenServiceTest`, `RefreshTokenServiceTest`, `UserServiceTest` (rotación, reutilización, revocación, inactivo 401, desactivar revoca), `AdminBootstrapTest`.
- **Clientes** (WireMock o `MockRestServiceServer`): 200; 404 → 422 o 403 según el caso; inactivo; 500, timeout y conexión rechazada → 503; el `Authorization` original llega al proveedor.
- **Integración**: `Ms*ApplicationTests` con `postgres:16` de Testcontainers, Flyway activo y `ddl-auto=validate`.
- **Gates**: `./mvnw -B verify` desde la carpeta de cada servicio, con `BUILD SUCCESS` (Checkstyle, PMD + CPD, SpotBugs con FindSecBugs, Dependency-Check, JaCoCo ≥ 70 %; en ms-users también en `service` y `controller`). Los resultados reales se anotan en `docs/SECURITY-REPORT.md` de cada servicio. Si no se pudo ejecutar Docker, `NVD_API_KEY` o red, el análisis queda "pendiente", nunca aprobado.

## 10. Riesgos aceptados y discrepancias abiertas

**Riesgos aceptados**

1. **Clave HS256 compartida** por los tres servicios: cualquiera podría emitir tokens. Aceptado para la beca (D5); alternativa futura: RS256 con clave pública.
2. **Access token vigente hasta 15 min** tras desactivar una cuenta. D13 lo cierra solo en las mutaciones de ms-catalog; ms-tickets no revalida `active` del solicitante.
3. **Sin rate limiting** en `/auth/login` ni CORS (D11).
4. **Swagger y `/v3/api-docs` públicos** por defecto; en producción `SWAGGER_ENABLED=false`.
5. **403 en lugar de 404** en recursos ajenos de ms-tickets revela que el ticket existe.
6. **Dependencia síncrona** ms-catalog → ms-users: si ms-users cae, las mutaciones de ms-catalog responden 503.
7. **Cambio de esquema y contrato de ms-users** (migraciones V2/V3, `{id}`, `fullName`, `active`): afecta a consumidores; se documenta en el PR.

**Pendiente de entorno**: Testcontainers requiere Docker; Dependency-Check requiere `NVD_API_KEY` y red; `JWT_SECRET` debe inyectarse en docker-compose y CI.

**Discrepancias abiertas**

- **Cancelación y cierre de tickets**: este documento sigue ARCHITECTURE §6.3 (dueño → CERRADO/CANCELADO; AGENTE asignado → EN_PROGRESO/RESUELTO; ADMIN todo). El plan de ms-tickets y su `requerimientos.md` (RN-05, RN-06, HU-04) proponen que solo el AGENTE asignado cambie el estado, y que ADMIN y SOLICITANTE reciban 403. Debe cerrarse con el equipo y actualizar el documento que corresponda (regla 14).
- **Fase de ms-catalog**: el plan de ms-catalog difiere D13 (validación de ADMIN activo); aquí se documenta como regla vigente por decisión del equipo, y el plan debe alinearse.
- **Códigos de error**: `DEPENDENCY_UNAVAILABLE` y los 422 deben quedar en la tabla de ARCHITECTURE §3.4.
