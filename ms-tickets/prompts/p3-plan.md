# Plan P3 — ms-tickets (Isaac)

Fuente de verdad: ARCHITECTURE.md (§3.3, §3.4, §6, §7) y docs/seguridad.md. Plan base: docs/planes/PLAN-SEGURIDAD-P3.md (corregido en este documento donde difiere de ARCHITECTURE).

Estado: **P1, sin código.** Espera aprobación antes de P2.

## 1. Decisiones ya tomadas

- **Cambio de estado (decidido por Isaac): se aplica ARCHITECTURE §6.3** y `docs/seguridad.md` §8.
  - SOLICITANTE dueño: puede CERRAR (desde RESUELTO) y CANCELAR (desde ABIERTO/ASIGNADO).
  - AGENTE asignado: EN_PROGRESO (desde ASIGNADO) y RESUELTO (desde EN_PROGRESO). CERRAR y CANCELAR = 403.
  - ADMIN: todas las transiciones y la asignación.
- Lo anterior **contradice** a `ms-tickets/docs/requerimientos.md` RN-06 y el escenario "Cancelación desde ABIERTO por el agente asignado" (líneas 146–149), y a la §7 decisión 2 de PLAN-SEGURIDAD-P3. Requiere actualizar esos documentos (ver §7).

## 2. Dependencias (Boot 4.1.2, Java 17)

Verificadas en Maven Central (`repo1.maven.org`, artefacto 4.1.2 existente):

| Dependencia | Alcance | Nota |
|---|---|---|
| `spring-boot-starter-security-oauth2-resource-server` | compile | Nombre de Boot 4 (ARCHITECTURE §6.1) |
| `spring-boot-starter-restclient` | compile | `RestClient` para `@HttpExchange` (ARCHITECTURE §7) |
| `spring-boot-starter-security-test` | test | `jwt()` de `spring-security-test` (ARCHITECTURE §6.2). Existe. |

Diferencia: PLAN-SEGURIDAD-P3 §2 usa `spring-boot-starter-security-oauth2-resource-server-test`; también existe en 4.1.2, pero ARCHITECTURE nombra `spring-boot-starter-security-test`. Propongo **seguir ARCHITECTURE**.

Sin Spring Cloud ni Feign (D4). Versiones gestionadas por el parent 4.1.2; no se fijan a mano.

## 3. Clases nuevas y modificadas

Nuevas (sin cambios respecto a PLAN-SEGURIDAD-P3 §3, salvo lo indicado):

| Clase | Paquete | Función |
|---|---|---|
| `SecurityConfig` | `config` | `SecurityFilterChain` stateless, CSRF off, `@EnableMethodSecurity`; públicos `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`; reglas por ruta (ver nota) |
| `JwtProperties` | `config/properties` | Record `app.security.jwt.*` (secreto, issuer, audience, access-ttl); falla al arrancar si el secreto mide < 32 bytes |
| `JwtDecoderConfig` | `config` | `NimbusJwtDecoder.withSecretKey` HS256 + validadores `iss=ms-users`, `aud=mesa-ayuda` |
| `JwtRolesConverter` | `security` | Claim `roles` → `ROLE_*` (un `scope` no otorga roles) |
| `ProblemAuthenticationEntryPoint` | `security` | 401 `UNAUTHORIZED` en ProblemDetail |
| `ProblemAccessDeniedHandler` | `security` | 403 `FORBIDDEN` en ProblemDetail |
| Resolver de `CurrentUser` | `security` | `CurrentUser(sub, roles)` desde el `Jwt` |
| `RestClientConfig` | `config` | Beans `CatalogClient` y `UsersClient` (`HttpServiceProxyFactory` + `RestClientAdapter`); timeouts connect 2 s / read 3 s; sin reintentos |
| `ClientsProperties` | `config/properties` | **Ya existe** (`ms-tickets/src/main/java/.../config/properties/ClientsProperties.java`): se adapta a `clients.catalog.url` / `clients.users.url` |
| `CatalogClient`, `UsersClient` | `client` | `@HttpExchange`: `GET /api/v1/categories/{id}`, `GET /api/v1/priorities/{id}`, `GET /api/v1/users/{id}` |
| `client/dto/` | `client` | DTOs propios de ms-tickets |
| `BearerTokenRelayInterceptor` | `client` | Copia el `Authorization` del `SecurityContext` |
| `ClientErrorHandler` | `client` | 404 proveedor → 422 de negocio; 401/403 proveedor, 5xx, `ResourceAccessException` → 503 (con WARN en 401/403) |
| `DependencyUnavailableException` | `exception` | 503 `DEPENDENCY_UNAVAILABLE` |

Modificadas:

| Clase / archivo | Cambio |
|---|---|
| `controller/TicketController` | Quitar los 6 `@RequestHeader`; recibir `CurrentUser` |
| `security/CurrentUser` | Pasa de `(Long userId, UserRole role)` a `(sub, roles)`; Javadoc actualizado |
| `service/TicketService` | Al crear valida categoría y prioridad (snapshot SLA); al asignar valida AGENTE activo en ms-users (§4.1) — **P5** |
| `service/TicketAccessPolicy` | Reglas de estado de §1 (cancelar y cerrar por SOLICITANTE dueño; AGENTE sin cerrar/cancelar) |
| `service/TicketStateMachine` | Sin cambios de mapa (ya es D6). Se revisa la pendiente de la bitácora #5 (ABIERTO → ASIGNADO solo por asignación) |
| `exception/GlobalExceptionHandler` | Quitar `MissingRequestHeaderException`; mapear 422 `CATEGORY_INVALID`, `PRIORITY_INVALID`, `ASSIGNEE_INVALID` y 503 `DEPENDENCY_UNAVAILABLE` |
| `application.yaml` | `app.security.jwt.*` con `${JWT_SECRET}` sin valor por defecto; `clients.*.url` con `${SERVICES_*_URL}`; timeouts; Actuator solo `health`, `show-details=never`; `include-message/stacktrace=never` |
| `templates/application-*.example.yaml` | Solo si ya lo tiene el servicio (ya existen): añadir `JWT_SECRET` y URLs |
| DTOs de entrada | Bean Validation revisado; sin mass assignment |
| `config/OpenApiConfig` y `docs/contratos/ms-tickets.yaml` | Esquema bearer, 401/403 (P6) |
| `TicketControllerTest`, `TicketServiceTest` | Reemplazar encabezados por `jwt()` / `CurrentUser` nuevo |
| `src/test/resources` | Secreto de prueba ≥ 32 caracteres (no el real) |

**Nota de diseño:** `SecurityConfig` solo puede decidir por ruta, no por el estado destino de `PATCH /status` (viene en el body). Por eso, para `PATCH /{id}/status` la regla de rol y de transición se aplica en el service (`TicketAccessPolicy`) con `CurrentUser`. Para los demás endpoints la regla de rol va en `SecurityConfig` (`hasRole`).

## 4. Matriz endpoint × rol (ARCHITECTURE §6.3)

Sin token = 401. Recurso ajeno = 403.

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN | Quién aplica la regla |
|---|---|---|---|---|---|
| `POST /api/v1/tickets` | 401 | 201 | 403 | 403 | `SecurityConfig` (`hasRole SOLICITANTE`) |
| `GET /api/v1/tickets` | 401 | 200 (propios) | 200 (asignados) | 200 (todos) | Service (filtro) |
| `GET /api/v1/tickets/{id}` | 401 | 200 dueño / 403 ajeno | 200 asignado / 403 ajeno | 200 | Service |
| `GET /api/v1/tickets/{id}/history` | 401 | 200 dueño / 403 ajeno | 200 asignado / 403 ajeno | 200 | Service |
| `PATCH /{id}/assignment` | 401 | 403 | 403 | 200 (ABIERTO o ASIGNADO) | `SecurityConfig` (`hasRole ADMIN`) + estado en service |
| `PATCH /{id}/status` → EN_PROGRESO (desde ASIGNADO) | 401 | 403 | 200 si asignado / 403 ajeno | 200 | Service |
| `PATCH /{id}/status` → RESUELTO (desde EN_PROGRESO) | 401 | 403 | 200 si asignado / 403 ajeno | 200 | Service |
| `PATCH /{id}/status` → CERRADO (desde RESUELTO) | 401 | 200 si dueño / 403 ajeno | 403 | 200 | Service |
| `PATCH /{id}/status` → CANCELADO (desde ABIERTO/ASIGNADO) | 401 | 200 si dueño / 403 ajeno | 403 | 200 | Service |
| `/actuator/health` | 200 | 200 | 200 | 200 | `SecurityConfig` (público) |

Diferencias contra el plan base (PLAN-SEGURIDAD-P3 §4): AGENTE → CANCELADO pasa de 200 a 403; SOLICITANTE → CERRADO/CANCELADO pasa de 403 a 200 (dueño); ADMIN pasa de 403 a 200 en status.

## 5. Pruebas que demuestran 401 / 403 / éxito

- Paquete `security/` con `@WebMvcTest` + `@Import(SecurityConfig.class)` y `@MockitoBean JwtDecoder`. Nunca se desactiva la seguridad.
- **401**: sin token, firma alterada, expirado, `iss` incorrecto, `aud` incorrecto → `code=UNAUTHORIZED`.
- **403**: prueba parametrizada que recorre cada celda 403 de §4 y el caso de recurso ajeno (SOLICITANTE y AGENTE no dueños/asignados). Se verifica que el service no se invoca si la regla de rol falla en `SecurityConfig`.
- **200/201**: caso feliz por rol y endpoint.
- `JwtRolesConverterTest`: el claim `roles` produce `ROLE_*`; un `scope` no otorga roles.
- `JwtPropertiesTest`: arranque falla con secreto < 32 bytes.
- Actuator: `/actuator/health` público; otros endpoints de Actuator no expuestos.
- Adaptar `TicketControllerTest` y `TicketServiceTest`.
- Clientes (WireMock, P5): 200; 404 categoría/prioridad → 422; asignado inexistente, inactivo o sin AGENTE → 422 `ASSIGNEE_INVALID`; 5xx, timeout, conexión rechazada → 503; 401/403 proveedor → 503 con WARN; el `Authorization` original se propaga.
- Verificación: `./mvnw -B verify` en verde (Checkstyle, PMD, SpotBugs, JaCoCo ≥ 70 %, Dependency-Check) sin PostgreSQL local.

Línea base medida hoy: `./mvnw -B verify` → BUILD SUCCESS, 17 pruebas, 58 s.

## 6. Riesgos

1. Reglas de rol en `SecurityConfig` y reglas de estado en service pueden divergir; la matriz §4 debe cubrir ambas.
2. `TicketAccessPolicy` crecerá con las reglas de estado; hay que mantener ≤ 30 líneas por método y complejidad ≤ 10 (Checkstyle/PMD).
3. Secreto HS256 compartido entre los 3 servicios (aceptado en D5).
4. 403 en lugar de 404 revela existencia de un ticket ajeno (lo exige la guía).
5. Si `BearerTokenRelayInterceptor` no copia el token, ms-users y ms-catalog responden 401 y ms-tickets lo mapea a 503.
6. Swagger público por defecto (`SWAGGER_ENABLED=true`).
7. Cobertura JaCoCo ≥ 70 %: el código nuevo de seguridad debe tener pruebas o el gate falla.
8. Un usuario desactivado conserva acceso con su access token hasta 15 min (riesgo aceptado).
9. Los quality gates pueden reportar hallazgos nuevos por el código de seguridad.

## 7. Decisiones y pendientes para Isaac

1. **Confirmar la matriz §4** (ARCHITECTURE §6.3), en particular AGENTE → CANCELADO = 403.
2. **Actualizar `ms-tickets/docs/requerimientos.md`**: RN-06 y el escenario de cancelación por el agente (líneas 146–149) contradicen esta matriz. ¿Lo actualizo en P6 o lo haces tú?
3. **Actualizar `ms-tickets/docs/planes/PLAN-SEGURIDAD-P3.md`** §4 y §7 decisión 2 para no dejar dos fuentes distintas. Propuesta: no tocarlo (es plan de referencia) y documentar la divergencia en el informe de seguridad.
4. **Dependencia de test**: ¿`spring-boot-starter-security-test` (ARCHITECTURE) o `...oauth2-resource-server-test` (plan)? Propongo ARCHITECTURE.
5. **Tag `p3-entrega-isaac` ya existe** en el repo. Hay que decidir si se mueve (ver sección 6 del prompt maestro) o se usa otro nombre al final.
6. **Contrato `docs/contratos/ms-tickets.yaml`**: las 6 rutas coinciden con ARCHITECTURE §3.3. La comparación detallada de DTOs y códigos de error se hace en P2 y se actualiza en P6.
7. **Secreto de prueba**: ¿valor fijo en `src/test/resources/application-test.yaml` o variable de entorno en CI? Propongo fijo solo en tests.
8. **Alcance**: el plan de P5 toca `ms-tickets` ↔ ms-users. Esa prueba se detiene antes de ejecutarse, como pediste.

## 8. Orden propuesto para P2 (tras aprobación)

1. Dependencias en `pom.xml`.
2. `JwtProperties`, `JwtDecoderConfig`, `JwtRolesConverter`, `ProblemAuthenticationEntryPoint`, `ProblemAccessDeniedHandler`, `SecurityConfig`.
3. `CurrentUser` nuevo y `TicketController` sin headers.
4. `TicketAccessPolicy` con las reglas de estado de §4.
5. `application.yaml` y `GlobalExceptionHandler`.
