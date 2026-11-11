---
name: seguridad
description: Agrega o audita la seguridad JWT y por roles (RBAC) de un microservicio Spring Boot 4 de este repositorio (ms-catalog, ms-tickets o ms-users) como resource server con Spring Security, siguiendo ARCHITECTURE.md §6 y el plan ms-catalog/docs/planes/PLAN-SEGURIDAD-P3.md. Úsala cuando el usuario pida proteger endpoints, validar tokens, aplicar la matriz endpoint-rol, responder 401/403 en ProblemDetail o escribir las pruebas de seguridad. No la uses para emitir tokens (login/refresh de ms-users) ni para las puertas de calidad (usa quality-gates-java).
---

# seguridad

Convierte un microservicio en resource server stateless que valida los JWT HS256 emitidos por ms-users y aplica la matriz de roles. Trabaja dentro de un único microservicio. Ejemplo de referencia: `ms-catalog/docs/planes/PLAN-SEGURIDAD-P3.md`.

## Fuentes de verdad

- `ARCHITECTURE.md`: D5, D7, D12, D13, §3.2/§3.3/§3.4 (contratos y errores), §6.1–6.4 (JWT, filter chain, matriz, secretos) y §7 (integración).
- `docs/seguridad.md` y `docs/contratos/ms-<servicio>.yaml`. Si el documento está vacío o contradice a `ARCHITECTURE.md`, detente y pregunta.
- Contrato del token (emisor ms-users): `iss=ms-users`, `aud=mesa-ayuda`, `sub=<id usuario>`, `roles=["SOLICITANTE"|"AGENTE"|"ADMIN"]`, expiración de 15 min, firma HS256 con los bytes UTF-8 de `JWT_SECRET` (≥ 32 bytes).

## Pasos

Ejecuta los comandos desde el directorio del microservicio.

1. **Estado previo.** `git status --short`. Si hay cambios sin relación, detente y pregunta.
2. **Plan aprobado.** Confirma que existe un plan en `docs/planes/` y que el usuario lo aprobó. Si no existe, genera uno (dependencias, clases, matriz endpoint × rol, pruebas, riesgos, decisiones) y espera aprobación antes de escribir código.
3. **Dependencias.** Antes de editar el `pom.xml`, muestra el diff y espera aprobación. Usa los nombres de Boot 4: `spring-boot-starter-security-oauth2-resource-server` y, en test, `spring-boot-starter-security-test`. Verifica que cada dependencia exista y compile; no inventes artefactos.
4. **Configuración.** Record `JwtProperties` (`app.security.jwt`: secret, issuer, audience) con `secret` solo desde `${JWT_SECRET}` y validación de ≥ 32 bytes al arrancar. Nunca pongas un secreto en claro en `application*.yaml`, plantillas, pruebas versionadas ni logs. Documenta las variables en `.env.example` y en `templates/`.
5. **Clases de seguridad** (`config/security`):
   - `SecurityConfig`: stateless, CSRF off, `oauth2ResourceServer().jwt()`, `@EnableMethodSecurity`. Públicos: `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`. Todo lo demás autenticado. `JwtDecoder` con `NimbusJwtDecoder.withSecretKey` (HS256) y validadores de `iss`, `aud` y expiración.
   - `JwtRolesConverter`: claim `roles` → `ROLE_<ROL>`; tolera claim ausente, no lista o elementos no texto.
   - Entry point (401 `UNAUTHORIZED`) y access denied (403 `FORBIDDEN`) en `application/problem+json`, reutilizando el constructor de ProblemDetail del `GlobalExceptionHandler`. Añade un handler de `AccessDeniedException` para que `@PreAuthorize` no caiga en el 500 genérico.
6. **Reglas de rol.** `@PreAuthorize("hasRole('ADMIN')")` en cada método de escritura de los controllers, según la matriz de `ARCHITECTURE.md` §6.3. No cambies rutas, métodos ni códigos HTTP definidos en las HU y los contratos.
7. **Matriz base de ms-catalog.** GET: cualquier usuario autenticado (200). POST/PUT/DELETE: solo ADMIN (SOLICITANTE y AGENTE → 403). Sin token → 401 en todos los endpoints protegidos.
8. **Pruebas.** Una prueba por celda de la matriz (endpoint × {sin token, SOLICITANTE, AGENTE, ADMIN}), con `jwt()` de spring-security-test:
   - 401: sin token. Además, con tokens reales firmados en la prueba: firma alterada, expirado, `iss` incorrecto y `aud` incorrecto; verifica `code=UNAUTHORIZED` y content type `application/problem+json`.
   - 403: SOLICITANTE y AGENTE en endpoints de escritura; verifica `code=FORBIDDEN` y `verifyNoInteractions(service)`.
   - 200/201/204: ADMIN y lectura con cualquier rol.
   - Unitarias de `JwtRolesConverter` y de `JwtProperties` (secret corto falla).
   - Ajusta los `@WebMvcTest` existentes importando `SecurityConfig` y autenticando como ADMIN. Esto es adaptar el contexto de prueba; no cambies las aserciones de negocio.
9. **Contrato y documentación.** `security: bearerAuth` y respuestas 401/403 en `docs/contratos/ms-<servicio>.yaml`, esquema `bearerAuth` en `OpenApiConfig`, `docs/seguridad.md` (claims, roles, TTL) y README del servicio.
10. **Verificación.** `./mvnw -B verify` (si falta `NVD_API_KEY`, usa `-Ddependency-check.skip=true` y registra Dependency-Check como pendiente con el motivo). Si fallan analizadores, aplica `quality-gates-java`.
11. **Fase B (solo ms-catalog, D13).** Cuando ms-users exponga `GET /api/v1/users/{id}`: `UsersClient` (`@HttpExchange` sobre `RestClient`, timeouts 2 s / 3 s, sin reintentos), relay del `Authorization` y guard de ADMIN activo solo en POST/PUT/DELETE. Mapeo: 200 con `active=true` → continúa; 404 o `active=false` → 403; 401/403 del proveedor, 5xx o timeout → 503 `DEPENDENCY_UNAVAILABLE`. Pruebas con WireMock o `MockRestServiceServer` para cada caso.

## Prohibiciones

- No registres en logs ni en respuestas contraseñas, tokens, `Authorization`, emails ni el secreto.
- No uses `permitAll` fuera de la lista pública, ni `anyRequest().permitAll()`, ni desactives la validación de `iss`, `aud` o expiración.
- No pongas `JWT_SECRET` ni llaves en archivos versionados, salvo valores de ejemplo evidentes en `.env.example`.
- No habilites CORS con `*` ni CSRF con sesiones; la API es stateless.
- No `@SuppressWarnings`, ni desactives reglas de Checkstyle, PMD, SpotBugs o JaCoCo para que pase el build, ni excluyas clases de JaCoCo.
- No modifiques una prueba existente para que pase; corrige el código.
- No inventes resultados: informa solo lo que se ejecutó.
- No ejecutes `git commit`, `git push`, `git tag` ni `gh pr create`. Entrega el comando al usuario.
- Si una corrección implica una dependencia desconocida, cambiar endpoints o códigos HTTP de las HU, o contradice `ARCHITECTURE.md`, no la apliques: avisa al usuario para registrarla como rechazo.

## Terminado cuando

- Existe una prueba de 401, 403 y éxito para cada endpoint protegido, y todas pasan.
- `./mvnw -B verify` termina en BUILD SUCCESS (o con Dependency-Check pendiente y su motivo) y JaCoCo ≥ 70 %.
- Contrato OpenAPI, `docs/seguridad.md` y `.env.example` reflejan la seguridad implementada.
- Se entregó al usuario el resumen: archivos tocados, resultado de `verify`, riesgos pendientes y el comando de commit sugerido.
