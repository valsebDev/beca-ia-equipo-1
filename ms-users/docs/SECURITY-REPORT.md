# Reporte de seguridad: ms-users (P3)

## 1. Matriz endpoint × rol

| Endpoint | SOLICITANTE | AGENTE | ADMIN | Sin token | Prueba que lo cubre |
|---|---|---|---|---|---|
| `GET /api/v1/users/me` | 200 | 200 | 200 | 401 | `UserControllerSecurityTest` |
| `POST /api/v1/auth/logout` | 204 | 204 | 204 | 401 | `AuthControllerSecurityTest` |
| `GET /api/v1/users` | 403 | 403 | 200 | 401 | `UserControllerSecurityTest` |
| `GET /api/v1/users/{id}` | 403 | 403 | 200 | 401 | `UserControllerSecurityTest`, `UserProviderContractTest` |
| `POST /api/v1/users` | 403 | 403 | 201 | 401 | `UserControllerSecurityTest` |
| `PATCH /api/v1/users/{id}/status` | 403 | 403 | 200 | 401 | `UserControllerSecurityTest` |
| `POST /api/v1/auth/register` | público | público | público | público | `AuthControllerSecurityTest` |
| `POST /api/v1/auth/login` | público | público | público | público | `AuthControllerSecurityTest` |
| `POST /api/v1/auth/refresh` | público | público | público | público | `AuthControllerSecurityTest`, `RefreshTokenServiceTest` |

Los tokens firmados mal, expirados o con `iss`/`aud` incorrectos responden 401 `UNAUTHORIZED` en formato ProblemDetail (`UserControllerSecurityTest`).

## 2. Qué corregí del plan

- La migración de usuarios pasó de V2 a V5, porque V2 ya es `refresh_tokens`. V1 a V4 no se editaron.
- Se retiró el bloqueo 423 por intentos fallidos y se eliminó `PUT /users/me`.
- Los códigos de error se alinearon con `ARCHITECTURE.md` §3.4.
- Se documentaron refresh, logout y administración (RN-09 a RN-13 y HU-USR-06 a 09).
- Se añadió `maxLength` a `LoginRequest.password` (72) y `RefreshRequest.refreshToken` (128) en `docs/contratos/ms-users.yaml`.

## 3. Hallazgos del revisor

Revisión de solo lectura con el agente `revisor-seguridad-ms-users` (herramientas `read` y `search`), contrastada con búsqueda directa en el código.

| ID | Severidad | OWASP | Archivo:línea | Hallazgo | Corrección / Estado |
|---|---|---|---|---|---|
| R-01 | BAJA | A05 Security Misconfiguration | `ms-users/src/main/java/com/ids/ms_users/exception/GlobalExceptionHandler.java` | Sin handler para 415 ni 405 | Corregido en P6 (`dc9f715`). 405 sin `code` con cabecera `Allow`; 415 `UNSUPPORTED_MEDIA_TYPE` (excepción documentada en §5). |
| R-02 | BAJA | A05 Security Misconfiguration | `ms-users/src/main/java/com/ids/ms_users/config/SecurityConfig.java` | CSRF deshabilitado en `/api/v1/**` | Aceptado. JWT sin estado y sin cookies de sesión. |
| SEC-01 | MEDIA | A07 Identification and Authentication Failures | `ms-users/src/main/java/com/ids/ms_users/service/RefreshTokenService.java:60` | `rotate` lee con `findByTokenHash` sin bloqueo. Dos peticiones simultáneas con el mismo refresh token podrían emitir dos tokens nuevos. | **Pendiente de verificación.** Sin prueba de concurrencia no se confirma el impacto. Propuesta: `@Lock(PESSIMISTIC_WRITE)` en la consulta y prueba de concurrencia. No se corrige en esta entrega. |
| SEC-02 | MEDIA | A07 Identification and Authentication Failures | `ms-users/src/main/java/com/ids/ms_users/controller/AuthController.java` (`register`) | El 409 de registro permite enumerar cuentas. | **Riesgo aceptado por especificación** (ARCHITECTURE §3.1 exige 409). Pendiente de decisión del equipo. |
| SEC-03 | BAJA | A05 Security Misconfiguration | `ms-users/src/main/java/com/ids/ms_users/exception/EmailAlreadyExistsException.java:7` y `GlobalExceptionHandler.java:32` | El `detail` del 409 incluye el email. | **No corregido.** El email lo envió el propio solicitante. Cambiarlo afecta al contrato de error. Pendiente de decisión del equipo. |
| SEC-04 | BAJA | A02 Cryptographic Failures | `ms-users/src/main/java/com/ids/ms_users/service/TokenService.java:48-49` | El access token (firmado, no cifrado) incluye `email` y `name`. | **Decisión del equipo** (`docs/seguridad.md` §2: "solo informativos"). Se documenta; el token no se cifra en esta fase. |
| SEC-05 | BAJA | A07 Identification and Authentication Failures | `ms-users/src/main/java/com/ids/ms_users/config/JwtDecoderConfig.java` | Un access token sigue válido hasta 15 min tras desactivar la cuenta. | **Riesgo aceptado** (ver §5). |
| SEC-06 | BAJA | N/A | `ms-users/src/test/java/com/ids/ms_users/controller/AuthControllerSecurityTest.java` | El caso de cuenta inactiva usa el mismo mock que la contraseña incorrecta. | **No corregido en esta entrega.** La cuenta inactiva está probada a nivel de servicio (`AuthServiceTest`). No se modifican pruebas existentes (regla 20). |
| SEC-07 | BAJA | N/A | `ms-users/src/test/java/com/ids/ms_users/security/UnmappedRouteSecurityTest.java:18` | `@WebMvcTest(controllers = UnmappedRouteSecurityTest.class)` apunta a la propia clase de prueba. | **Pendiente.** La prueba pasa, pero el valor de `controllers` debe revisarse. No se modifica para no tocar una prueba existente. |
| SEC-08 | BAJA | N/A | `ARCHITECTURE.md` §6.2 y `ms-users/pom.xml` | §6.2 declara públicas `/v3/api-docs/**` y `/swagger-ui/**`, pero el proyecto no tiene springdoc. | **Divergencia documental.** Decisión pendiente del equipo. `ARCHITECTURE.md` no se modifica. |
| SEC-09 | BAJA | N/A | `ms-users/prompts/p3-plan.md` §1 frente a §0 | §1 todavía menciona `PUT /me`, que se retiró. | **Pendiente de corrección documental.** El código no mapea PUT (`putMeIsNoLongerMappedEvenWithValidToken`). |
| SEC-10 | BAJA | N/A | `ms-users/src/main/java/com/ids/ms_users/config/SecurityConfig.java:39` | Divergencia sobre si `POST /auth/logout` es público. | **Sin cambio.** Código y matriz (`p3-plan.md` §4) coinciden: logout exige autenticación (ARCHITECTURE §3.1). |

Verificado sin hallazgos: control de acceso según la matriz (`@PreAuthorize` en `UserController`), `SecurityConfig` (rutas públicas y `anyRequest` con `UnmappedRouteAuthorizationManager`), validación JWT (firma HS256, `iss`, `aud`, `exp`, `nbf`), `JwtRolesConverter`, secretos sin valor por defecto, `server.error.include-*` en `never`, ProblemDetail en 401/403/500, refresh opaco guardado como SHA-256 con revocación por reuso, y logs sin contraseñas, tokens ni emails.

Pendiente de revisión por esta pasada: entidades `User` y `RefreshToken`, `UserMapper`, Bean Validation de `RefreshRequest` y `LogoutRequest`, y el contrato `docs/contratos/ms-users.yaml`.

## 4. Qué corregí

- Validaciones `@Size` en los DTOs de entrada.
- Logout con control de propiedad (`revokeOwned`). Mantiene 204 para tokens ajenos, inválidos o ya revocados.
- Mitigación de tiempo en login con hash ficticio cuando el email no existe. Mantiene el 401 genérico.
- Guardia de placeholder en `AdminBootstrap`, para no crear un admin con credenciales sin resolver.
- Prueba de contrato del proveedor para `GET /api/v1/users/{id}` (`UserProviderContractTest`, 4 casos).

## 5. Qué decidí no corregir

- **R-01**: corregido en P6. Se añadió el handler de `HttpMediaTypeNotSupportedException` (415 `UNSUPPORTED_MEDIA_TYPE`); el 405 ya tenía handler.
- **Excepción a ARCHITECTURE §3.4**: el código `UNSUPPORTED_MEDIA_TYPE` (415) no está en la tabla de §3.4. Se mantiene como excepción documentada; `ARCHITECTURE.md` no se modifica en esta entrega.
- **R-02**: con JWT sin estado y sin cookies de sesión, CSRF no aplica. Se documenta, no se cambia.
- **Divergencia de configuración**: `ms-users` usa `@EnableConfigurationProperties` en los consumidores, mientras que `ms-catalog` y `ms-tickets` usan `@ConfigurationPropertiesScan`. `@ConfigurationPropertiesScan` no registra beans en los slices de `@WebMvcTest`.
- **Excepción D13**: la prueba de contrato del proveedor depende de `GET /api/v1/users/{id}` (ADMIN). Se documenta en `p3-plan.md`.

### Riesgos aceptados

- **Llave HS256 compartida** entre servicios. Cualquier servicio que la tenga puede firmar tokens válidos. Mitigación: rotación de secretos y `JWT_SECRET` sin valor por defecto.
- **Access token vigente hasta 15 min** después de desactivar a un usuario. La validación de estado se hace al emitir el token, no en cada petición.
- **Integración entre servicios (rúbrica, 10 pts)**: N/A para `ms-users`. Solo es proveedor del JWT y de `GET /api/v1/users/{id}`. No consume ningún servicio (D13 en `docs/seguridad.md`), así que no hay cliente `RestClient` ni propagación de token. La parte de proveedor está cubierta por `UserProviderContractTest`. Pendiente confirmar con el profesor.
- **SEC-01 (MEDIA, carrera en refresh)**: no se corrige sin prueba de concurrencia que lo reproduzca. Queda como pendiente.
- **SEC-02 (MEDIA, enumeración en registro)**: riesgo aceptado por especificación (ARCHITECTURE §3.1).
- **SEC-03 y SEC-04 (BAJA)**: decisiones documentadas en §3.
- **Revisor**: ningún hallazgo CRÍTICO ni ALTO confirmado. No se requiere corrección obligatoria.
- **Revisión de PR ajeno (D8)**: diferida por decisión del usuario.
- **Contrato consumidor (D8)**: pendiente de acordar con el equipo.


## 6. Evidencia `./mvnw -B verify`

Ejecutado desde `ms-users/` (`.\mvnw.cmd -B verify`) después de la prueba `UserControllerJwtAuthorizationTest` (15 casos, commit `29e3c54`):

```
[INFO] Tests run: 104, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Historial: 88 tests antes del handler 415; 89 tras añadirlo (`dc9f715`); 104 con las pruebas `jwt()`.
