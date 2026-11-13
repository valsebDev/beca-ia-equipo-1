# P3 — Plan de seguridad de ms-catalog (Sebastián)

Fuentes: [ARCHITECTURE.md](../../ARCHITECTURE.md) §3.2, §6, §7 (D13) y [docs/seguridad.md](../../docs/seguridad.md) §3, §5.2, §7, §9.
Este plan no contiene código. Los puntos marcados **DECISIÓN** requieren confirmación antes de P2.

## 0. Hallazgos de P0 (resumen)

- `./mvnw -B verify` en verde salvo **Dependency-Check** (`NVD_API_KEY` vacía, pendiente de entorno). Checkstyle 0 violaciones, SpotBugs 0, 52 tests OK, JaCoCo línea 96,1 %.
- No hay dependencias de seguridad en el `pom.xml` ni clases de seguridad. `docs/contratos/ms-catalog.yaml` tiene `security: []`.
- `GlobalExceptionHandler` tiene un `@ExceptionHandler(Exception.class)` que devolvería **500** a un `AccessDeniedException`. Hay que añadir un manejador 403.
- No existe `src/test/resources/`. Los `application-local.yaml`, `application-prod.yaml` y las dos plantillas `templates/*.example.yaml` están **vacíos** (0 bytes).

## 1. Dependencias (pom.xml) — requiere confirmación (AGENTS / instrucciones del repo)

Versiones gestionadas por el parent 4.1.2; ambas existen en Maven Central (verificado).

| Dependencia | Alcance | Motivo |
|---|---|---|
| `spring-boot-starter-security-oauth2-resource-server` | compile | `NimbusJwtDecoder`, `oauth2ResourceServer().jwt()`. Trae `spring-security-config` y `spring-security-web` |
| `spring-boot-starter-security-test` | test | `jwt()` de `spring-security-test` en pruebas |

No se añade JJWT ni ninguna librería de JWT extra.

## 2. Configuración

`src/main/resources/application.yaml` (valores sin secretos):

```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET}      # sin valor por defecto
      issuer: ${JWT_ISSUER:ms-users}
      audience: ${JWT_AUDIENCE:mesa-ayuda}
```

- `JWT_SECRET` solo desde entorno. El arranque falla si tiene menos de 32 bytes UTF-8.
- `issuer` y `audience` van en `application.yaml` con valores por defecto, porque son constantes del contrato (no secretos).
- Pruebas: `src/test/resources/application-test.yaml` (nuevo) con un secreto **de prueba** de 32+ caracteres. Nunca el real.
- `springdoc` y `management` ya están configurados. Sin cambios.

## 3. Clases nuevas y modificadas (según seguridad.md §3, §5.2 y ARCHITECTURE §6.2)

Todas las clases nuevas usan constructor manual (sin Lombok, regla del repo para `security/` y `config/`). Jackson 3 (`tools.jackson`).

| Clase | Paquete | Qué hace |
|---|---|---|
| `SecurityConfig` (nueva) | `config` | `SecurityFilterChain` stateless, CSRF off, `@EnableMethodSecurity`, `oauth2ResourceServer().jwt()` con entry point y access denied propios. Públicos: `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`. Resto: `authenticated()` |
| `JwtDecoderConfig` (nueva) | `config` | `NimbusJwtDecoder.withSecretKey(...)` HS256 + validadores de `iss`, `aud` y expiración |
| `JwtProperties` (nueva) | `config.properties` | `record` `@ConfigurationProperties("app.security.jwt")` con validación de longitud del secreto en el constructor compacto (32 bytes). Se registra con el `@ConfigurationPropertiesScan` que ya existe |
| `JwtRolesConverter` (nueva) | `security` | Lee solo el claim `roles` y lo convierte en `ROLE_<VALOR>`. Tolera claim ausente o tipo inválido. `scope` no otorga roles |
| `ProblemAuthenticationEntryPoint` (nueva) | `security` | 401 `UNAUTHORIZED` en `ProblemDetail` (`application/problem+json`) |
| `ProblemAccessDeniedHandler` (nueva) | `security` | 403 `FORBIDDEN` en `ProblemDetail` |
| `GlobalExceptionHandler` (modificada) | `exception` | Añade `@ExceptionHandler(AccessDeniedException.class)` → 403 `FORBIDDEN`, para que un `@PreAuthorize` denegado no caiga en el 500 genérico |
| `CategoryController`, `PriorityController` | `controller` | **Sin cambios.** La regla de rol vive en `SecurityConfig` (decisión de Sebastián en P1), no en `@PreAuthorize` |
| `OpenApiConfig` | `config` | Sin cambios en P2 |

## 4. Matriz endpoint × rol (ARCHITECTURE §6.3 y seguridad.md §5.2)

| Endpoint | Sin token (esperado) | SOLICITANTE | AGENTE | ADMIN | Prueba que lo cubre |
|---|---|---|---|---|---|
| `GET /api/v1/categories`, `GET /{id}` | 401 | 200 | 200 | 200 | `CategoryControllerSecurityTest` |
| `POST /api/v1/categories` | 401 | 403 | 403 | 201 | `CategoryControllerSecurityTest` |
| `PUT /api/v1/categories/{id}` | 401 | 403 | 403 | 200 | `CategoryControllerSecurityTest` |
| `DELETE /api/v1/categories/{id}` | 401 | 403 | 403 | 204 | `CategoryControllerSecurityTest` |
| `GET /api/v1/priorities`, `GET /{id}` | 401 | 200 | 200 | 200 | `PriorityControllerSecurityTest` |
| `POST /api/v1/priorities` | 401 | 403 | 403 | 201 | `PriorityControllerSecurityTest` |
| `PUT /api/v1/priorities/{id}/sla` | 401 | 403 | 403 | 200 | `PriorityControllerSecurityTest` |
| `GET /actuator/health` | 200 | 200 | 200 | 200 | `ActuatorSecurityTest` |
| Otras rutas `/actuator/*` | 401 (sin token) | 404 | 404 | 404 | `ActuatorSecurityTest` |

En cada 403 se verifica con `verifyNoInteractions(service)` que el service no se invoca.

## 5. Pruebas (P2 ajusta las existentes; P3 añade las de seguridad)

- **401** (`SecurityTokenValidationTest`, con `JwtDecoder` real): sin token, firma alterada, expirado, `iss` incorrecto, `aud` incorrecto → `UNAUTHORIZED` en ProblemDetail.
- **403 y éxito**: matrices de §4 en las dos clases `*ControllerSecurityTest`.
- **Roles**: `JwtRolesConverterTest` (lista válida, vacía, ausente, tipo inválido, minúsculas; `scope` no otorga roles). Se demuestra que sin el converter daría 403.
- **Configuración**: `JwtPropertiesTest` (secreto de 31 bytes falla; de 32 pasa).
- **Actuator**: `ActuatorSecurityTest` (health público; otras rutas no expuestas).
- **Existentes**: `CategoryControllerTest`, `CategoryErrorResponseTest`, `PriorityControllerTest` pasan a `@Import(SecurityConfig.class)` y `jwt()`. **No se cambian aserciones.**
- `MsCatalogApplicationTests` (Testcontainers `postgres:16`) recibe `JWT_SECRET` de prueba vía `application-test.yaml`.
- Cobertura JaCoCo ≥ 70 % y `./mvnw -B verify` en verde (Dependency-Check queda pendiente sin `NVD_API_KEY`).

## 6. Fuera de este plan (no toca ms-users)

- **D13 (P5)**: `AdminGuard` que consulta `GET /api/v1/users/{sub}` en ms-users antes de POST/PUT/DELETE, `UsersClient`, `BearerTokenRelayInterceptor` y pruebas WireMock. **Se detiene antes de P5** para confirmar la comunicación con ms-users.
- No se toca `docs/contratos/ms-catalog.yaml` hasta P6 (`bearerAuth`, 401/403).
- No se toca `docker-compose.yml`, `.env.example` raíz ni `docs/seguridad.md`.

## 7. Riesgos

- Configuración de checkstyle/PMD: métodos ≤ 30 líneas, complejidad ≤ 10, líneas ≤ 120, sin números mágicos fuera de constantes ni `application.yaml`.
- Sin `@SuppressWarnings` ni exclusiones de analizadores.
- Un fallo de configuración de seguridad podría dejar endpoints abiertos: la matriz de §4 es la lista de verificación.

## 8. DECISIONES que necesito de Sebastián

1. **Dependencias**: confirmar las dos del punto 1 (el repo exige confirmación para cambios de `pom.xml`).
2. **Dónde va la regla de rol**: propongo `@PreAuthorize` en los controllers (seguridad.md §5.2). La alternativa es por ruta en `SecurityConfig` (ARCHITECTURE §6.3). No se mezclan.
3. **`GlobalExceptionHandler`**: confirmar el manejador 403 para `AccessDeniedException`, sin el cual un 403 respondería 500.
4. **Pruebas existentes**: confirmar que añadir `@Import(SecurityConfig.class)` y `jwt()` a las pruebas `@WebMvcTest` es aceptable (no cambia aserciones, según la regla 20 del repo).
5. **Plantillas vacías**: `templates/application-*.example.yaml` y `application-local/prod.yaml` están vacíos. Propongo **no** rellenarlos en P2; el `JWT_SECRET` se documenta en el README del servicio en P6. ¿Estás de acuerdo?
6. **Nombres de variables de BD**: `docs/seguridad.md` §8 usa `SPRING_DATASOURCE_USERNAME/PASSWORD`, mientras que ARCHITECTURE §6.4 y `application.yaml` usan `CATALOG_DB_USER/PASSWORD`. Propongo mantener `CATALOG_DB_*` (lo que ya está en código) y avisar al equipo para corregir `seguridad.md` (fuera de mi alcance).
7. **Cambios sin commitear en el árbol**: `docs/seguridad.md` (vacío en HEAD, con contenido sin commitear), `.gitignore`, `.github/skills/seguridad/SKILL.md` y `ms-catalog/docs/planes/PLAN-SEGURIDAD-P3.md` borrados, y planes nuevos sin seguir. No los toco; confírmame si son intencionales antes de cualquier commit.
8. **Discrepancias para el equipo (no bloquean P2)**:
   - `docs/contratos/ms-users.yaml` usa `/api/v1/users/{userIdentifier}/status`, pero ARCHITECTURE §6.3 pide `{id}`. Se revisará en P5 con Fernando.
   - ARCHITECTURE §3.2 muestra rutas sin prefijo `/api/v1`, mientras el contrato y el código sí lo usan. Se toma `/api/v1` como válido.
