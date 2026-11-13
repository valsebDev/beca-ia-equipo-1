# Reporte de seguridad P3 — ms-tickets

Servicio: `ms-tickets` (Spring Boot 4.1.2, Java 17). Fuente de verdad: `ARCHITECTURE.md` (§6.3 matriz de autorización, §3.4 errores, §7 propagación). Plan: `prompts/p3-plan.md`.

## 1. Matriz endpoint → rol

Cada endpoint exige JWT (`bearerAuth`). Sin token o con token inválido responde `401`. La regla de rol se aplica en `SecurityConfig` y la de propiedad en `TicketAccessPolicy`.

| Endpoint | Rol que puede | Notas |
|---|---|---|
| `POST /api/v1/tickets` | SOLICITANTE | ARCHITECTURE §6.3: ADMIN y AGENTE reciben `403`. El solicitante queda como dueño |
| `GET /api/v1/tickets` | Según rol (propios / asignados / todos) | Filtra por visibilidad |
| `GET /api/v1/tickets/{id}` | Dueño, agente asignado, ADMIN | Otro usuario recibe `403` |
| `PATCH /api/v1/tickets/{id}/assignment` | ADMIN | Valida agente con ms-users (ver §5) |
| `PATCH /api/v1/tickets/{id}/status` | Según transición (ver §2) | Matriz de ARCHITECTURE §6.3 |
| `GET /api/v1/tickets/{id}/history` | Dueño, agente asignado, ADMIN | Otro usuario recibe `403` |

## 2. Correcciones al plan

Estas cuatro correcciones siguen ARCHITECTURE §6.3 y están aplicadas en el código. `requerimientos.md` RN-05 y RN-06 y HU-TCK-04 ya coinciden con esa matriz.

1. **AGENTE → CANCELADO = 403.** El plan lo permitía; ARCHITECTURE §6.3 y la implementación lo rechazan con `403`.
2. **SOLICITANTE dueño cierra o cancela.** `CERRADO` desde `RESUELTO` y `CANCELADO` desde `ABIERTO` o `ASIGNADO`, solo para el dueño. El plan lo dejaba solo para el AGENTE.
3. **ADMIN cambia el estado.** Puede ejecutar cualquier transición válida. El plan no lo contemplaba.
4. **Dependencia de pruebas:** `spring-boot-starter-security-test` (`pom.xml`), la que indica ARCHITECTURE. El plan decía `oauth2-resource-server-test`.

Pendiente: marcar la casilla "Plan corregido… marcando tus cambios" en la sección 7 de `prompts/p3-plan.md`.

## 3. Hallazgos del revisor (re-ejecución del 2026-11-12)

Esta sección sustituye la tabla anterior SEC-01 a SEC-11. Ese registro no se conservó con el texto original, así que no se reconstruyó. Esta tabla viene de una nueva revisión de solo lectura (`revisor-seguridad-ms-tickets`, OWASP A01–A09) sobre el código actual. Ningún hallazgo es CRITICAL ni HIGH.

| ID | Severidad | OWASP | Archivo:línea | Hallazgo | Estado |
|---|---|---|---|---|---|
| SEC-01 | BAJO | A01 | `service/TicketService.java:88-89`, `:118-119`, `:156-158` | Oráculo de existencia: `load()` responde `404 TICKET_NOT_FOUND` antes de comprobar acceso, así que un usuario sin relación recibe `404` o `403` según exista el id. Con ids secuenciales permite enumerar tickets. | No corregido. Corrección sugerida: responder igual en ambos casos |
| SEC-02 | BAJO | A05 | `resources/application.yaml:5-6`, `:27-29` | Valores por defecto para la URL de la base de datos (`localhost:5434`), el usuario `tickets` y las URLs de servicios. La contraseña (`:7`) y `JWT_SECRET` (`:35`) no tienen default. | No corregido. Corrección sugerida: quitar defaults fuera de un perfil de desarrollo |
| SEC-03 | BAJO | A01 / A04 | `security/CurrentUser.java:28-33`, `security/JwtRolesConverter.java:19-21`, `config/SecurityConfig.java:32-33` | Con varios roles, el filtro concede todos, pero `CurrentUser` toma el primero según el orden del enum. No hay escalada: el rol de menor privilegio gana y falla cerrado. El comportamiento no está especificado. | No corregido. Corrección sugerida: rechazar tokens con más de un rol reconocido |
| SEC-04 | BAJO | A05 / A09 | `exception/GlobalExceptionHandler.java:74-77` | El manejador genérico `Exception.class` registra con stack trace errores como 405 o 415, que no tienen handler propio, así que un cliente puede generar logs ruidosos. **Inferido del código; no ejecutado.** | No corregido. Corrección sugerida: handlers específicos (405, 415) a nivel WARN sin stack trace |

Categorías revisadas sin hallazgos: A03 (sin SQL concatenado), A04 (DTO de creación con Bean Validation; el cliente no fija `owner`, `status` ni `assigneeId`), A06 (Dependency-Check reporta 0 vulnerabilidades; no se verificó que el escaneo fuera completo), A09 (sin logs de tokens, contraseñas ni correos en `src/main`).

## 4. Qué se corrigió

- Autenticación JWT como resource server (`JwtDecoderConfig`, `JwtRolesConverter`, handlers de 401/403, `SecurityConfig`, `CurrentUser`).
- Validación de `iss` y `aud`, algoritmo fijo HS256, rechazo de `none`, expiración.
- `JWT_SECRET` sin valor por defecto; el arranque falla si mide menos de 32 bytes (`JwtDecoderConfig.java:28-34`).
- `server.error.include-stacktrace=never` e `include-message=never`.
- Clientes HTTP reales hacia ms-users y ms-catalog (`ClientsConfig`, `RemoteUserGateway`, `RemoteCatalogGateway`, `UsersClient`, `CatalogClient`) con `RestClient`: timeouts configurables, propagación del Bearer token, y errores según ARCHITECTURE §3.4 y §7 (provider 404 → 422; 401/403/5xx/timeout/conexión → 503 `DEPENDENCY_UNAVAILABLE`).
- Excepciones de dominio con causa (`DomainException`, `BusinessRuleException`, `DependencyUnavailableException`), corrigiendo 6 violaciones de PMD `PreserveStackTrace`.
- Pruebas: 69 tests al cierre de P5, incluidos los de WireMock en `RemoteClientsTest`.

## 5. Qué no se corrigió y por qué

- **Prueba de contrato contra `docs/contratos/ms-users.yaml`:** pendiente. Requiere que `GET /api/v1/users/{id}` esté en `main` (ms-users) y no se ha ejecutado contra el servicio real.
- Hallazgos SEC-01 a SEC-04 de la §3: ninguno es CRITICAL ni HIGH, y la decisión de P3 es corregir solo CRITICAL y HIGH.

## 6. Riesgos aceptados

- **Clave HS256 compartida** entre ms-users (emisor) y ms-tickets (validador). Decisión D5 del plan.
- **Access token válido hasta 15 minutos** después de desactivar un usuario, porque el JWT no se revoca en tiempo real.
- **Sin rate limiting en `/auth/login`.** Es responsabilidad de ms-users; no se modifica desde ms-tickets.
- **Transición de estados sin validación de rol específica** (riesgo medio del registro anterior): `TicketAccessPolicy.requireTransition` valida propiedad y asignación, no el rol por transición. Se revisará en la siguiente iteración.

## 7. Evidencia de verificación

- Re-ejecución del revisor de seguridad sobre el código en `ebf8f24`, solo lectura, sin cambios en el código.
- `./mvnw -B verify`: CI `ci-isaac` sobre `ebf8f24`, 69 tests, 0 fallos (https://github.com/valsebDev/beca-ia-equipo-1/actions/runs/43155129535).

## 8. Pendientes

1. Decidir si se corrigen SEC-01 a SEC-04 en P4 o se aceptan como riesgo.
2. Revisar si el exclude de SpotBugs `SPRING_CSRF_PROTECTION_DISABLED` (`spotbugs-exclude.xml`) puede estrecharse.
