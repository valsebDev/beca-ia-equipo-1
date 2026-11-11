# Plan: seguridad en ms-catalog (P3)

## Contexto verificado
- Fuente de reglas: `ARCHITECTURE.md` (raíz): D5 (JWT HS256, `JWT_SECRET`), D7, D12 (sin librería común), D13 (ms-catalog → ms-users), §3.2, §3.4, §6.1–6.4, §7, backlog 16, 20b y 22.
- Estado inicial de ms-catalog: sin Spring Security, todo público. Controllers `CategoryController` (`/api/v1/categories`) y `PriorityController` (`/api/v1/priorities`). Errores en ProblemDetail con `GlobalExceptionHandler` y `FrameworkExceptionHandler`. Actuator solo `health`; Swagger activable con `SWAGGER_ENABLED`.
- ms-users ya emite JWT con JJWT: `iss=ms-users`, `aud=mesa-ayuda`, `sub=id`, `roles=["ADMIN"]`, llave = bytes UTF-8 de `JWT_SECRET` (≥ 32 bytes). Es compatible con `NimbusJwtDecoder.withSecretKey`. Roles: SOLICITANTE, AGENTE, ADMIN.
- Los tests actuales son `@WebMvcTest` sin seguridad (hay que ajustarlos) y `MsCatalogApplicationTests` con Testcontainers.

## Fase A — Resource server

### Dependencias (`pom.xml`)
- `spring-boot-starter-security-oauth2-resource-server`.
- `spring-boot-starter-security-test` (scope test).

### Clases
Nuevas:
- `config/properties/JwtProperties`: record `app.security.jwt` (secret, issuer, audience). Falla el arranque si el secret mide menos de 32 bytes.
- `config/security/SecurityConfig`: `SecurityFilterChain` stateless, CSRF off, `oauth2ResourceServer().jwt()`, `@EnableMethodSecurity`. Públicos: `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`; el resto autenticado. `JwtDecoder` HS256 con validadores de `iss`, `aud` y expiración.
- `config/security/JwtRolesConverter`: claim `roles` → `ROLE_<ROL>` (tolera claim ausente o de tipo inválido).
- `config/security/ProblemDetailSecurityHandler`: `AuthenticationEntryPoint` (401 `UNAUTHORIZED`) y `AccessDeniedHandler` (403 `FORBIDDEN`) en `application/problem+json`.

Modificadas:
- `CategoryController` y `PriorityController`: `@PreAuthorize("hasRole('ADMIN')")` en POST/PUT/DELETE.
- `GlobalExceptionHandler`: helpers `unauthorized()`/`forbidden()` y handler de `AccessDeniedException` (403 en vez de 500).
- `application.yaml`: bloque `app.security.jwt` (secret solo por `${JWT_SECRET}`); plantillas en `templates/`.
- `OpenApiConfig`: esquema `bearerAuth`.
- `docs/contratos/ms-catalog.yaml`: `security: bearerAuth` y respuestas 401/403.
- `docs/seguridad.md` y README del servicio.

### Matriz endpoint × rol (ARCHITECTURE §3.2 / §6.3)
| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN |
|---|---|---|---|---|
| GET `/api/v1/categories`, `/{id}` | 401 | 200 | 200 | 200 |
| POST `/api/v1/categories` | 401 | 403 | 403 | 201 |
| PUT `/api/v1/categories/{id}` | 401 | 403 | 403 | 200 |
| DELETE `/api/v1/categories/{id}` | 401 | 403 | 403 | 204 |
| GET `/api/v1/priorities`, `/{id}` | 401 | 200 | 200 | 200 |
| POST `/api/v1/priorities` | 401 | 403 | 403 | 201 |
| PUT `/api/v1/priorities/{id}/sla` | 401 | 403 | 403 | 200 |
| GET `/actuator/health`, `/v3/api-docs`, `/swagger-ui` | 200 | 200 | 200 | 200 |
| Otras rutas de actuator | 401 | — | — | — |

### Pruebas (401 / 403 / 200)
- `SecurityTokenValidationTest` (MockMvc con `JwtDecoder` real y tokens firmados en el test): sin token, firma alterada, expirado, `iss` incorrecto y `aud` incorrecto → 401 ProblemDetail (`code=UNAUTHORIZED`).
- `CategoryControllerSecurityTest` y `PriorityControllerSecurityTest` (`@WebMvcTest` + `@Import(SecurityConfig)` + `jwt()` de spring-security-test): matriz parametrizada endpoint × {sin token, SOLICITANTE, AGENTE, ADMIN} → 401/403/2xx. En 403 se verifica que el service nunca se invoca (`verifyNoInteractions`).
- `JwtRolesConverterTest`: lista válida, vacía, ausente, tipo inválido, minúsculas.
- `JwtPropertiesTest`: secret < 32 bytes falla.
- Ajustar `CategoryControllerTest`, `PriorityControllerTest` y `CategoryErrorResponseTest` (importar seguridad y autenticar como ADMIN). `MsCatalogApplicationTests` recibe un `JWT_SECRET` de prueba.
- Meta: `./mvnw -B verify` en verde (Checkstyle, PMD, SpotBugs/FindSecBugs, Dependency-Check, JaCoCo ≥ 70 %).

## Fase B — DIFERIDA (decisión del usuario): validar ADMIN activo contra ms-users (D13, backlog 20b)
Fuera del alcance de esta entrega; se documenta como riesgo aceptado en `docs/SECURITY-REPORT.md`.
- Dependencia: `spring-boot-starter-restclient` (+ WireMock o `MockRestServiceServer` en test).
- Clases: `client/UsersClient` (`@HttpExchange GET /api/v1/users/{id}`), `client/dto/UserSummary`, `config/ClientsConfig` (timeouts 2 s / 3 s), `BearerTokenRelayInterceptor`, `ClientErrorHandler`, `service/AdminGuard` (solo en mutaciones) y `DependencyUnavailableException` → 503 `DEPENDENCY_UNAVAILABLE`.
- Mapeo: 200 con `active=true` → continúa; 404 o `active=false` → 403; 401/403/5xx/timeout → 503. Los GET no llaman a ms-users.
- Pruebas: 200, 404, inactivo, 500, timeout y conexión rechazada; tests de service con cliente mockeado.
- Config nueva: `clients.users.url` ← `SERVICES_USERS_URL`.

## Riesgos
1. ms-users aún no está completo con Spring Security (usa JJWT); la Fase B no se puede probar E2E hasta que lo entregue. Mitigación: contrato `docs/contratos/ms-users.yaml` + WireMock.
2. Boot 4.1.2: nombres de starters y paquetes cambiados (Jackson 3, `webmvc-test`). Verificar compilación temprano.
3. Llave HS256 compartida: cualquier servicio podría emitir tokens (riesgo aceptado D5; documentar en SECURITY-REPORT).
4. Access token de 15 min: un ADMIN desactivado conserva acceso hasta su expiración; D13 lo cierra solo en mutaciones.
5. Swagger y `/v3/api-docs` públicos (§6.2); en prod conviene `SWAGGER_ENABLED=false`.
6. Regresión de tests existentes, cobertura, PMD y Checkstyle por las clases nuevas.
7. Dependency-Check puede marcar CVEs (requiere `NVD_API_KEY`).
8. Sin rate limiting (D11) y sin CORS (§6.2).
9. ms-users debe permitir a ADMIN `GET /users/{id}` para la Fase B (§6.3 ya lo permite).

## Decisiones tomadas (confirmadas por el usuario)
1. Solo Fase A; Fase B diferida. Dependencias `oauth2-resource-server` y `security-test` aprobadas.
2. GET para cualquier usuario autenticado; compatibilidad con claims de ARCHITECTURE §6.1 sin tocar ms-users.
Por defecto, salvo que se indique lo contrario:
2. Reglas de rol con `@PreAuthorize` en controllers.
3. Swagger y `/v3/api-docs` públicos (§6.2), apagados en prod con `SWAGGER_ENABLED=false`.
4. AGENTE y SOLICITANTE solo leen.
5. Pruebas: `jwt()` de spring-security-test para la matriz y tokens firmados reales para los casos 401.
6. Rama `practica-3-sebastian`, commits por fase.

## Tareas
- [ ] A1 pom + `JwtProperties` + validación de secret
- [ ] A2 `SecurityConfig`, `JwtDecoder`, `JwtRolesConverter`
- [ ] A3 Handlers 401/403 ProblemDetail + handler global de `AccessDeniedException`
- [ ] A4 `@PreAuthorize` en controllers
- [ ] A5 Pruebas 401/403/200, conversor y ajuste de tests existentes
- [ ] A6 Config yaml/templates, OpenAPI, contrato, `docs/seguridad.md`, README
- [ ] A7 `./mvnw -B verify` en verde
- [ ] B (diferida) Cliente ms-users, guard de ADMIN activo, pruebas WireMock
