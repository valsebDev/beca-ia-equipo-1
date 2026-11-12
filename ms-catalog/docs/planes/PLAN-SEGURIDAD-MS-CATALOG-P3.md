# Plan y ejecución: seguridad en ms-catalog (P3)

> Estado actualizado después de alinear la simulación P5/D13 con el contrato
> de ms-users. La integración contra una instancia real sigue pendiente hasta
> que se fusione la implementación de ms-users correspondiente a ese contrato.

## Contexto inicial (antes de implementar)
- Fuente de reglas: `ARCHITECTURE.md` (raíz): D5 (JWT HS256, `JWT_SECRET`), D7, D12 (sin librería común), D13 (ms-catalog → ms-users), §3.2, §3.4, §6.1–6.4, §7, backlog 16, 20b y 22.
- Estado inicial de ms-catalog: sin Spring Security, todo público. Controllers `CategoryController` (`/api/v1/categories`) y `PriorityController` (`/api/v1/priorities`). Errores en ProblemDetail con `GlobalExceptionHandler` y `FrameworkExceptionHandler`. Actuator solo `health`; Swagger activable con `SWAGGER_ENABLED`.
- ms-users ya emite JWT con JJWT: `iss=ms-users`, `aud=mesa-ayuda`, `sub=id`, `roles=["ADMIN"]`, llave = bytes UTF-8 de `JWT_SECRET` (≥ 32 bytes). Es compatible con `NimbusJwtDecoder.withSecretKey`. Roles: SOLICITANTE, AGENTE, ADMIN.
- Los tests iniciales eran `@WebMvcTest` sin seguridad; `MsCatalogApplicationTests` ya usaba Testcontainers.

## Fase A — Resource server (completada)

### Dependencias (`pom.xml`)
- `spring-boot-starter-security-oauth2-resource-server`.
- `spring-boot-starter-security-test` (scope test).

### Clases
Nuevas:
- `config/properties/JwtProperties`: record `app.security.jwt` (secret, issuer, audience). Falla el arranque si el secret mide menos de 32 bytes.
- `config/SecurityConfig`: `SecurityFilterChain` stateless, CSRF off, `oauth2ResourceServer().jwt()` y `@EnableMethodSecurity`. Públicos: `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`; el resto requiere autenticación. `JwtDecoderConfig` valida HS256, `iss`, `aud` y expiración.
- `security/JwtRolesConverter`: claim `roles` → `ROLE_<ROL>` (tolera claim ausente o de tipo inválido).
- `security/ProblemResponseWriter`: genera ProblemDetail para `AuthenticationEntryPoint` (401 `UNAUTHORIZED`) y `AccessDeniedHandler` (403 `FORBIDDEN`).

Modificadas:
- `SecurityConfig`: autorización por ruta con `requestMatchers(...).hasRole("ADMIN")` para las mutaciones. Aunque `@EnableMethodSecurity` está activo, **no se usa `@PreAuthorize`**; la regla de rol se aplica en `SecurityConfig`.
- `CategoryController` y `PriorityController`: llaman a `AdminGuard` antes de cada POST/PUT/DELETE (D13).
- `GlobalExceptionHandler`: maneja `AccessDeniedException` como 403 y `DependencyUnavailableException` como 503.
- `application.yaml`: bloque `app.security.jwt` (secret solo por `${JWT_SECRET}`); `JWT_SECRET` se proporciona mediante el entorno.
- `OpenApiConfig` y `docs/contratos/ms-catalog.yaml`: esquema `bearerAuth`; el contrato documenta autenticación y respuestas 401/403.
- `docs/contratos/ms-catalog.yaml`: actualizado para `bearerAuth` y respuestas 401/403.

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
- `CategoryControllerSecurityTest` y `PriorityControllerSecurityTest` (`@WebMvcTest` + `@Import(SecurityConfig)` + `jwt()` de spring-security-test): matriz endpoint × {sin token, SOLICITANTE, AGENTE, ADMIN} → 401/403/2xx. En 403 se verifica que el service nunca se invoca (`verifyNoInteractions`).
- `JwtRolesConverterTest`: lista válida, vacía, ausente, tipo inválido, minúsculas.
- `JwtPropertiesTest`: secret < 32 bytes falla.
- Ajustar los tests MVC existentes y proporcionar `JWT_SECRET` de prueba a `MsCatalogApplicationTests`.
- Resultado de verificación tras P5: `./mvnw -B verify` — BUILD SUCCESS, 108 tests, 0 violaciones de Checkstyle, PMD y SpotBugs; se cumplieron los checks de JaCoCo (cobertura total superior al 70 %).

## Fase B — Validar ADMIN activo contra ms-users (D13, implementada; integración real pendiente)

- Cliente: `client/UsersClient` (`@HttpExchange GET /api/v1/users/{id}`) con DTO propio `client/dto/UserClientResponse` que consume `active` de la respuesta documentada en `docs/contratos/ms-users.yaml`.
- Configuración: `ClientsProperties` y `UsersClientConfig`; `clients.users.url` ← `SERVICES_USERS_URL`, timeouts de conexión/lectura de 2 s / 3 s. Se usa `RestClient` y `SimpleClientHttpRequestFactory` de `spring-web`; no se agregó `spring-boot-starter-restclient`.
- Propagación: `BearerTokenRelayInterceptor` reenvía el JWT de `SecurityContextHolder` en `Authorization`.
- Guard: `security/AdminGuard` obtiene el `sub` del JWT y consulta ms-users antes de las mutaciones. No se creó `ClientErrorHandler`; el mapeo se realiza en el guard.
- Excepciones: `DependencyUnavailableException` y `GlobalExceptionHandler` producen 503 `DEPENDENCY_UNAVAILABLE`.
- Mapeo aplicado: 200 con `active=true` → continúa; 200 con `active=false` o 404 → 403 `FORBIDDEN`; 401/403 del proveedor, 5xx, timeout o conexión rechazada → 503 `DEPENDENCY_UNAVAILABLE`. Las rutas GET no invocan el guard ni llaman a ms-users.
- Dependencia de pruebas aprobada: `org.wiremock:wiremock-standalone:3.13.2` en scope test.
- Pruebas: `AdminGuardTest` (7 pruebas con Mockito) y `UsersClientWireMockTest` (8 pruebas con WireMock: respuesta 200 conforme al esquema del contrato, inactivo, 404, 401, 500, conexión cerrada, timeout y propagación de `Authorization`). La simulación sigue el contrato actualizado; no se ha llamado a una instancia real de ms-users.
- **Pendiente:** el código de ms-users disponible en el workspace aún no coincide con el contrato actualizado: el contrato usa `id`/`active` y el código actual devuelve `userIdentifier`/`isActive`. El usuario confirmó que fusionará después el código actualizado; realizar la prueba de integración real cuando ese merge esté disponible.

## Riesgos
1. El código de ms-users disponible aún no implementa la forma del contrato actualizado. Mitigación temporal: simular ese contrato con WireMock; probar contra el proveedor después de fusionar su código correspondiente.
2. Boot 4.1.2: nombres de starters y paquetes cambiados (Jackson 3, `webmvc-test`). Verificar compilación temprano.
3. Llave HS256 compartida: cualquier servicio podría emitir tokens (riesgo aceptado D5; documentar en SECURITY-REPORT).
4. Access token de 15 min: un ADMIN desactivado conserva acceso hasta su expiración; D13 lo cierra solo en mutaciones.
5. Swagger y `/v3/api-docs` públicos (§6.2); en prod conviene `SWAGGER_ENABLED=false`.
6. La verificación local de P5 terminó en verde; falta validar el contrato/proveedor real cuando el equipo confirme su disponibilidad.
7. Dependency-Check puede marcar CVEs (requiere `NVD_API_KEY`).
8. Sin rate limiting (D11) y sin CORS (§6.2).
9. `AdminGuard` se invoca desde los controllers antes del service; revisar que esta ubicación respete la separación de capas definida para el servicio.

## Decisiones tomadas (confirmadas por el usuario)
1. Se aprobaron las dependencias de `oauth2-resource-server` y `security-test`; posteriormente también WireMock para probar D13.
2. Los GET permiten cualquier usuario autenticado; se validan claims conforme a `ARCHITECTURE.md` sin modificar ms-users.
3. Las reglas de rol se aplican mediante `requestMatchers(...).hasRole("ADMIN")` en `SecurityConfig`, no mediante `@PreAuthorize` en los controllers.
4. Swagger y `/v3/api-docs` son públicos (§6.2); su desactivación en producción se controla con `SWAGGER_ENABLED`.
5. AGENTE y SOLICITANTE solo leen.
6. Se usan `jwt()` de spring-security-test para la matriz de roles y tokens firmados en los casos de validación 401.
7. El usuario aprobó implementar el guard D13 y las pruebas WireMock; la integración real queda pendiente hasta que se fusione la implementación de ms-users correspondiente al contrato actualizado.
8. Se aprobó la excepción SpotBugs para CSRF deshabilitado: API stateless que recibe JWT por `Authorization`, no por cookies.

## Tareas
- [x] A1 pom + `JwtProperties` + validación de secret
- [x] A2 `SecurityConfig`, `JwtDecoderConfig`, `JwtRolesConverter`
- [x] A3 Handlers 401/403 ProblemDetail + handler global de `AccessDeniedException`
- [x] A4 Autorización ADMIN en `SecurityConfig` para POST/PUT/DELETE (se sustituyó el enfoque propuesto de `@PreAuthorize`)
- [x] A5 Pruebas 401/403/200, conversor y ajuste de tests existentes
- [x] A6 Configuración YAML, OpenAPI y contrato; documentación de seguridad y README según el alcance aplicado
- [x] A7 `./mvnw -B verify` en verde (108 tests tras implementar D13)
- [x] B Cliente ms-users, guard de ADMIN activo, pruebas unitarias y WireMock
- [ ] Revisar la ubicación de `AdminGuard` respecto a la capa controller → service
- [x] Alinear la simulación WireMock con el contrato actualizado (`id`, `active`)
- [ ] Probar la integración contra ms-users después de fusionar el código correspondiente al contrato
