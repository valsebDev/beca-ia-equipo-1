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

| ID | Severidad | OWASP | Archivo | Estado |
|---|---|---|---|---|
| R-01 | BAJA | A05 Security Misconfiguration | `ms-users/src/main/java/com/ids/ms_users/exception/GlobalExceptionHandler.java` | Abierto. Errores MVC (415, 405) responden 500 `INTERNAL_ERROR`. |
| R-02 | BAJA | A05 Security Misconfiguration | `ms-users/src/main/java/com/ids/ms_users/config/SecurityConfig.java` | Aceptado. CSRF deshabilitado en `/api/v1/**` sin comentario. Es aceptable con JWT sin estado. |

No hay hallazgos ALTA ni MEDIA. Ningún secreto tiene valor por defecto, y no se registran contraseñas, tokens ni emails en logs.

## 4. Qué corregí

- Validaciones `@Size` en los DTOs de entrada.
- Logout con control de propiedad (`revokeOwned`). Mantiene 204 para tokens ajenos, inválidos o ya revocados.
- Mitigación de tiempo en login con hash ficticio cuando el email no existe. Mantiene el 401 genérico.
- Guardia de placeholder en `AdminBootstrap`, para no crear un admin con credenciales sin resolver.
- Prueba de contrato del proveedor para `GET /api/v1/users/{id}` (`UserProviderContractTest`, 4 casos).

## 5. Qué decidí no corregir

- **R-01**: fuera del alcance de P3. Queda como deuda conocida.
- **R-02**: con JWT sin estado y sin cookies de sesión, CSRF no aplica. Se documenta, no se cambia.
- **Divergencia de configuración**: `ms-users` usa `@EnableConfigurationProperties` en los consumidores, mientras que `ms-catalog` y `ms-tickets` usan `@ConfigurationPropertiesScan`. `@ConfigurationPropertiesScan` no registra beans en los slices de `@WebMvcTest`.
- **Excepción D13**: la prueba de contrato del proveedor depende de `GET /api/v1/users/{id}` (ADMIN). Se documenta en `p3-plan.md`.

### Riesgos aceptados

- **Llave HS256 compartida** entre servicios. Cualquier servicio que la tenga puede firmar tokens válidos. Mitigación: rotación de secretos y `JWT_SECRET` sin valor por defecto.
- **Access token vigente hasta 15 min** después de desactivar a un usuario. La validación de estado se hace al emitir el token, no en cada petición.
- **Sin rate limiting en `/auth/login`**. Queda para una fase posterior, fuera del alcance de P3.

## 6. Evidencia `./mvnw -B verify`

Ejecutado desde `ms-users/` sobre el commit `9fc90c9`:

```
[INFO] Results:
[INFO] Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  02:56 min
```
