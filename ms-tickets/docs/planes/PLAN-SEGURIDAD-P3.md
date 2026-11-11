# Plan de seguridad P3 — ms-tickets

Fuente de verdad: `ARCHITECTURE.md` (§0 D5/D11/D12, §5.2, §6.1–6.4). Estado: borrador pendiente de revisión; no incluye código.

## 1. Situación actual

`TicketController` recibe la identidad en los encabezados `X-User-Id` y `X-User-Role` (simulada). `TicketAccessPolicy` ya aplica reglas de rol y de propiedad con `CurrentUser`. No hay Spring Security en el `pom.xml`.

## 2. Dependencias a agregar (Boot 4.1.2, Java 17)

| Dependencia | Alcance | Nota |
|---|---|---|
| `spring-boot-starter-security` | compile | Verificar contra la guía de migración de Boot 4 |
| `spring-boot-starter-security-oauth2-resource-server` | compile | Nombre de Boot 4 (el anterior está deprecado) |
| `spring-boot-starter-security-test` | test | Aporta `jwt()` de `spring-security-test` |

No se agregan clientes HTTP reales en este paso (ver decisión 3).

## 3. Clases nuevas o modificadas

Nuevas:

| Clase | Paquete | Función |
|---|---|---|
| `SecurityConfig` | `config` | `SecurityFilterChain` stateless, CSRF off, `@EnableMethodSecurity`; públicos `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`; resto autenticado |
| `JwtProperties` | `config/properties` | `app.security.jwt.*` (secreto, issuer, audience); falla al arrancar si el secreto mide < 32 bytes |
| `JwtDecoderConfig` | `config` | `NimbusJwtDecoder.withSecretKey` HS256 + validadores `iss=ms-users` y `aud=mesa-ayuda` |
| `JwtRolesConverter` | `security` | Claim `roles` → `ROLE_*` (roles ≠ scope) |
| `ProblemAuthenticationEntryPoint` | `security` | 401 `UNAUTHORIZED` en ProblemDetail |
| `ProblemAccessDeniedHandler` | `security` | 403 `FORBIDDEN` en ProblemDetail |
| Resolver de `CurrentUser` | `security` | Construye `CurrentUser(sub, rol)` desde el JWT |

Modificadas:

| Clase / archivo | Cambio |
|---|---|
| `TicketController` | Quitar `@RequestHeader`; `@PreAuthorize` por rol; recibir `CurrentUser` |
| `CurrentUser` | Actualizar Javadoc (ya no es simulada) |
| `GlobalExceptionHandler` | Quitar `MissingRequestHeaderException` si ya no aplica |
| `application.yaml` | `app.security.jwt.*` con `${JWT_SECRET}` (sin valor en claro) |
| `OpenApiConfig` y `docs/contratos/ms-tickets.yaml` | Esquema bearer, respuestas 401/403 |
| `TicketControllerTest` | Reemplazar encabezados por `jwt()` |
| `src/test/resources` | Secreto de prueba ≥ 32 caracteres |

`TicketAccessPolicy` y `TicketService` conservan la regla de propiedad; `requireRole` queda como defensa en profundidad.

## 4. Matriz endpoint × rol

Sin token = 401. Recurso ajeno = 403.

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN |
|---|---|---|---|---|
| `POST /api/v1/tickets` | 401 | 201 | 403 | 403 |
| `GET /api/v1/tickets` | 401 | 200 (propios) | 200 (asignados) | 200 (todos) |
| `GET /api/v1/tickets/{id}` | 401 | 200 si es dueño, 403 si no | 200 si es asignado, 403 si no | 200 |
| `GET /api/v1/tickets/{id}/history` | 401 | 200 si es dueño, 403 si no | 200 si es asignado, 403 si no | 200 |
| `PATCH /{id}/assignment` | 401 | 403 | 403 | 200 (ABIERTO o ASIGNADO) |
| `PATCH /{id}/status` → EN_PROGRESO / RESUELTO | 401 | 403 | 200 si es asignado | 200 |
| `PATCH /{id}/status` → CERRADO | 401 | 200 si es dueño | 403 | 200 |
| `PATCH /{id}/status` → CANCELADO | 401 | 200 si es dueño | 403 (ver decisión 2) | 200 |
| `/actuator/health` | 200 | 200 | 200 | 200 |

## 5. Pruebas que demuestran 401 / 403 / éxito

Paquete `security/` con `MockMvc` y `jwt()`; nunca se desactiva la seguridad.

- **401**: sin token, firma alterada, token expirado, `iss` incorrecto, `aud` incorrecto → ProblemDetail con `code=UNAUTHORIZED`.
- **403**: prueba parametrizada que recorre cada celda 403 de la matriz, más recurso ajeno para SOLICITANTE y AGENTE.
- **200/201**: caso feliz por rol y endpoint.
- `JwtRolesConverterTest`: el claim `roles` produce `ROLE_*` (y un `scope` no otorga roles).
- Arranque falla con `JWT_SECRET` < 32 bytes.
- Adaptar `TicketControllerTest` y `TicketServiceTest`.
- Verificación: `./mvnw -B verify` en verde (Checkstyle, PMD, SpotBugs, JaCoCo ≥ 70 %, Dependency-Check).

## 6. Riesgos

1. Nombres de starters y paquetes de Boot 4 pueden diferir de lo documentado por la guía/IA.
2. Secreto HS256 compartido en los 3 servicios (aceptado en D5).
3. 403 en vez de 404 revela la existencia de un ticket ajeno (lo exige la guía).
4. Reglas duplicadas entre `@PreAuthorize` y el service pueden divergir.
5. Gateways simulados (`SimulatedUserGateway`, catálogo) no propagan el token.
6. Swagger público por defecto (`SWAGGER_ENABLED=true`) expone el contrato.
7. `JWT_SECRET` debe inyectarse en docker-compose y CI; no se modifican en este alcance.
8. Un usuario desactivado conserva su access token hasta 15 min.
9. Quality gates pueden reportar hallazgos nuevos por el código de seguridad.

## 7. Decisiones que debe tomar el autor

1. Rol con `@PreAuthorize` (recomendado) o en el `SecurityFilterChain`.
2. ¿El AGENTE asignado puede CANCELAR? El código y el Gherkin de HU-04 dicen sí; §6.3 dice 403.
3. ¿Mantener gateways simulados o implementar clientes reales con `BearerTokenRelayInterceptor`?
4. Swagger: público o solo con `SWAGGER_ENABLED`.

## 8. Correcciones del autor

_(Vacío: se completa tras la revisión.)_
