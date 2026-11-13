# Plan de seguridad P3 — ms-tickets

Fuentes: `requerimientos.md` de ms-tickets (reglas de estado RN-05/RN-06 y HU-04) y `ARCHITECTURE.md` (§0 D4/D5/D9/D11/D12/D14, §5.1–5.2, §6.1–6.4, §7, §8 fases 2–4). Estado: listo para iniciar la práctica 3; no incluye código.

## 1. Situación actual

`TicketController` recibe la identidad en los encabezados `X-User-Id` y `X-User-Role` (simulada). `TicketAccessPolicy` ya aplica reglas de rol y de propiedad con `CurrentUser`. No hay Spring Security en el `pom.xml`.

## 2. Dependencias a agregar (Boot 4.1.2, Java 17)

| Dependencia | Alcance | Nota |
|---|---|---|
| `spring-boot-starter-security-oauth2-resource-server` | compile | Nombre de Boot 4 (el anterior está deprecado); §6.1 |
| `spring-boot-starter-restclient` | compile | `RestClient` para los clientes `@HttpExchange` (§7) |
| `spring-boot-starter-security-oauth2-resource-server-test` | test | Nombre de Spring Initializr; aporta `jwt()` de `spring-security-test` (§6.2) |

Sin Spring Cloud ni Feign (D4).

## 3. Clases nuevas o modificadas

Nuevas:

| Clase | Paquete | Función |
|---|---|---|
| `SecurityConfig` | `config` | `SecurityFilterChain` stateless, CSRF off, `@EnableMethodSecurity`; públicos `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`; reglas de rol por ruta con `hasRole` (§6.3); resto autenticado (§6.2) |
| `JwtProperties` | `config/properties` | `app.security.jwt.*` (secreto, issuer, audience); falla al arrancar si el secreto mide < 32 bytes |
| `JwtDecoderConfig` | `config` | `NimbusJwtDecoder.withSecretKey` HS256 + validadores `iss=ms-users` y `aud=mesa-ayuda` |
| `JwtRolesConverter` | `security` | Claim `roles` → `ROLE_*` (roles ≠ scope) |
| `ProblemAuthenticationEntryPoint` | `security` | 401 `UNAUTHORIZED` en ProblemDetail |
| `ProblemAccessDeniedHandler` | `security` | 403 `FORBIDDEN` en ProblemDetail |
| Resolver de `CurrentUser` | `security` | Construye `CurrentUser(sub, roles)` desde el JWT; `roles` es el arreglo del claim (ARCHITECTURE §5.2, §6.1) |
| `RestClientConfig` | `config` | Beans `CatalogClient` y `UsersClient` con `HttpServiceProxyFactory` + `RestClientAdapter`; `baseUrl` desde `ClientsProperties`; timeouts connect 2 s / read 3 s; sin reintentos (§7) |
| `ClientsProperties` | `config/properties` | Record `@ConfigurationProperties` para `clients.catalog.url` (`SERVICES_CATALOG_URL`) y `clients.users.url` (`SERVICES_USERS_URL`), según ARCHITECTURE.md línea 67 |
| `CatalogClient`, `UsersClient` | `client` | Interfaces `@HttpExchange`: `GET /api/v1/categories/{id}`, `GET /api/v1/priorities/{id}`, `GET /api/v1/users/{id}` |
| `client/dto/` | `client` | DTOs propios de ms-tickets, sin reutilizar clases del proveedor (§7) |
| `BearerTokenRelayInterceptor` | `client` | `ClientHttpRequestInterceptor` que copia el `Authorization` del `SecurityContext` (§6.1) |
| `ClientErrorHandler` | `client` | `defaultStatusHandler`: 404 del proveedor → 422 de negocio; 401/403 del proveedor → 503 con log WARN; 5xx y `ResourceAccessException` → 503 (§3.4) |
| `DependencyUnavailableException` | `exception` | Mapea a 503 `DEPENDENCY_UNAVAILABLE` (§3.4) |

Modificadas:

| Clase / archivo | Cambio |
|---|---|
| `TicketController` | Quitar `@RequestHeader`; recibir `CurrentUser`; sin `@PreAuthorize` (la regla de rol está en `SecurityConfig`; propiedad y estado en service) |
| `TicketService` | Al crear valida categoría y prioridad contra ms-catalog y copia el SLA (snapshot); al asignar valida que el asignado exista, esté activo y tenga rol AGENTE en ms-users (§4.1, D9, D10) |
| `CurrentUser` | Actualizar Javadoc (ya no es simulada) |
| `GlobalExceptionHandler` | Quitar `MissingRequestHeaderException` si ya no aplica; mapear 422 `CATEGORY_INVALID` / `PRIORITY_INVALID` / `ASSIGNEE_INVALID` y 503 `DEPENDENCY_UNAVAILABLE` (§3.4) |
| `application.yaml` | `app.security.jwt.*` con `${JWT_SECRET}` (sin valor en claro); `clients.*.url` con `${SERVICES_*_URL}`; timeouts; `management.endpoints.web.exposure.include=health` y `show-details=never`; `server.error.include-message=never` e `include-stacktrace=never` (§6.2) |
| DTOs de entrada | Bean Validation (`@NotBlank`, `@Size`, `@Positive`) y sin mass assignment (§6.2) |
| Repositorios | Solo Spring Data o `@Query` con parámetros `:param`; sin concatenar SQL (§6.2) |
| CORS (en `SecurityConfig`) | Deshabilitado por defecto; si se necesita, `CorsConfigurationSource` con `CORS_ALLOWED_ORIGINS` y nunca `*` (§6.2) |
| Logs | Nunca registrar contraseñas, tokens, `Authorization` ni emails (§6.2) |
| `OpenApiConfig` y `docs/contratos/ms-tickets.yaml` | Esquema bearer, respuestas 401/403 |
| `TicketControllerTest` | Reemplazar encabezados por `jwt()` |
| `src/test/resources` | Secreto de prueba ≥ 32 caracteres |

`TicketAccessPolicy` y `TicketService` aplican la regla de estado de requerimientos.md (RN-06): solo el AGENTE asignado cambia el estado; SOLICITANTE y ADMIN reciben 403. La regla de propiedad se mantiene para `GET` por id e historial. `requireRole` queda como defensa en profundidad.

## 4. Matriz endpoint × rol

Sin token = 401. Recurso ajeno = 403.

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN |
|---|---|---|---|---|
| `POST /api/v1/tickets` | 401 | 201 | 403 | 403 |
| `GET /api/v1/tickets` | 401 | 200 (propios) | 200 (asignados) | 200 (todos) |
| `GET /api/v1/tickets/{id}` | 401 | 200 si es dueño, 403 si no | 200 si es asignado, 403 si no | 200 |
| `GET /api/v1/tickets/{id}/history` | 401 | 200 si es dueño, 403 si no | 200 si es asignado, 403 si no | 200 |
| `PATCH /{id}/assignment` | 401 | 403 | 403 | 200 (ABIERTO o ASIGNADO) |
| `PATCH /{id}/status` → EN_PROGRESO / RESUELTO | 401 | 403 | 200 si es asignado | 403 (RN-06: ADMIN no ejecuta trabajo) |
| `PATCH /{id}/status` → CERRADO | 401 | 403 | 200 si es asignado (desde RESUELTO) | 403 (RN-06) |
| `PATCH /{id}/status` → CANCELADO | 401 | 403 | 200 si es asignado (desde ABIERTO/ASIGNADO; HU-04, RN-05) | 403 (RN-06) |
| `/actuator/health` | 200 | 200 | 200 | 200 |

## 5. Pruebas que demuestran 401 / 403 / éxito

Paquete `security/` con `MockMvc` y `jwt()`; nunca se desactiva la seguridad.

- **401**: sin token, firma alterada, token expirado, `iss` incorrecto, `aud` incorrecto → ProblemDetail con `code=UNAUTHORIZED`.
- **403**: prueba parametrizada que recorre cada celda 403 de la matriz, más recurso ajeno para SOLICITANTE y AGENTE.
- **200/201**: caso feliz por rol y endpoint.
- `JwtRolesConverterTest`: el claim `roles` produce `ROLE_*` (y un `scope` no otorga roles).
- Arranque falla con `JWT_SECRET` < 32 bytes.
- Adaptar `TicketControllerTest` y `TicketServiceTest`.
- Según la guía: pruebas de controlador con `@WebMvcTest` + `@Import(SecurityConfig.class)` y `@MockitoBean JwtDecoder` (no se desactiva la seguridad).
- **Clientes (WireMock)**: 200 del proveedor; categoría o prioridad inexistente → 422 `CATEGORY_INVALID` / `PRIORITY_INVALID`; asignado inexistente, inactivo o sin rol AGENTE → 422 `ASSIGNEE_INVALID`; 5xx, timeout o conexión rechazada → 503 `DEPENDENCY_UNAVAILABLE`; 401/403 del proveedor → 503 con log WARN.
- **Propagación**: la llamada al proveedor lleva el `Authorization` original del usuario (`BearerTokenRelayInterceptor`).
- Verificación: `./mvnw -B verify` en verde (Checkstyle, PMD, SpotBugs, JaCoCo ≥ 70 %, Dependency-Check).

## 6. Riesgos

1. Nombres de starters y paquetes de Boot 4 pueden diferir de lo documentado por la guía/IA.
2. Secreto HS256 compartido en los 3 servicios (aceptado en D5).
3. 403 en vez de 404 revela la existencia de un ticket ajeno (lo exige la guía).
4. Reglas de rol en `SecurityConfig` y reglas de propiedad/estado en el service pueden divergir; los tests de la matriz (§5) deben cubrir ambas.
5. Si `BearerTokenRelayInterceptor` no copia el token, ms-users y ms-catalog responden 401 y ms-tickets lo mapea a 503.
6. Swagger público por defecto (`SWAGGER_ENABLED=true`) expone el contrato.
7. `JWT_SECRET` debe inyectarse en docker-compose y CI; no se modifican en este alcance.
8. Un usuario desactivado conserva su access token hasta 15 min. ARCHITECTURE solo revalida `active` en ms-catalog (D13); ms-tickets no revalida al requester (aceptado por el equipo).
9. Quality gates pueden reportar hallazgos nuevos por el código de seguridad.

## 7. Decisiones

1. **Cerrada por la guía y ARCHITECTURE §6.3.** Reglas de rol por ruta en `SecurityFilterChain` (como en el ejemplo de la guía, con `@EnableMethodSecurity` activo). Propiedad y estado (dueño, asignado, transición) en el service con `CurrentUser`.
2. **Cerrada por requerimientos.md (RN-05, RN-06, HU-04).** Cambio de estado solo por el AGENTE asignado; el AGENTE asignado puede cancelar desde ABIERTO/ASIGNADO (200). SOLICITANTE y ADMIN reciben 403 al cambiar estado. Esto diverge de ARCHITECTURE §6.3 (AGENTE → CANCELADO = 403; ADMIN ✔); la divergencia se reporta como discrepancia abierta. Pendiente: quién puede cancelar (requerimientos.md, pregunta 1).
3. **Cerrada por ARCHITECTURE D4, D9 y §7.** Clientes reales `CatalogClient` y `UsersClient` con `BearerTokenRelayInterceptor`. No se mantienen gateways simulados.
4. **Cerrada por ARCHITECTURE §6.2.** `/swagger-ui/**` y `/v3/api-docs/**` son públicos; `SWAGGER_ENABLED` vale `true` por defecto.
5. **Cerrada por el equipo.** ms-tickets no revalida `active` del requester (riesgo 8 aceptado).

## 9. Entregables de P3 (ARCHITECTURE §5.1, D14, backlog 25)

- `ms-tickets/docs/SECURITY-REPORT.md` con los hallazgos CRÍTICOS y ALTOS y su corrección.
- `.github/agents/revisor-seguridad-ms-tickets.agent.md` (revisor separado del implementador).
- `docs/contratos/ms-tickets.yaml` con esquema bearer y respuestas 401/403.
- Tag `p3-entrega-<nombre>` (D14).
- `docs/seguridad.md` (claims, roles, TTL, propagación) lo escribe el equipo (backlog 14); no es entregable de este plan.

## 8. Correcciones del autor

_(Vacío: se completa tras la revisión.)_