# Reporte de seguridad P3 — ms-tickets

Servicio: `ms-tickets` (Spring Boot 4.1.2, Java 17). Fuente de verdad: `ARCHITECTURE.md` (§6.3 matriz de estados, §3.4 errores, §7 propagación). Plan: `prompts/p3-plan.md`.

## 1. Matriz endpoint → rol

Cada endpoint exige JWT (`bearerAuth`). Sin token o con token inválido responde `401`. La autorización por rol y propiedad se aplica en `TicketAccessPolicy`.

| Endpoint | Rol que puede | Notas |
|---|---|---|
| `POST /api/v1/tickets` | SOLICITANTE, ADMIN | El solicitante queda como dueño del ticket |
| `GET /api/v1/tickets` | Según rol (propiedad / asignación / ADMIN) | Filtra por visibilidad |
| `GET /api/v1/tickets/{id}` | Dueño, agente asignado, ADMIN | Otro usuario recibe `403` |
| `PATCH /api/v1/tickets/{id}/assignment` | ADMIN | Valida agente con ms-users (ver §4) |
| `PATCH /api/v1/tickets/{id}/status` | Ver §2, divergencia con PLAN §4 | Transiciones según ARCHITECTURE §6.3 |
| `GET /api/v1/tickets/{id}/history` | Dueño, agente asignado, ADMIN | Otro usuario recibe `403` |

Las celdas exactas por estado están en `prompts/p3-plan.md` §4 (línea ~69). Las pruebas de `TicketControllerTest` cubren 401 en los seis endpoints y 200/403 por rol y dueño.

## 2. Correcciones al plan

- **Transición de estado (decisión tomada):** ARCHITECTURE §6.3 indica que el SOLICITANTE dueño cierra o cancela, y que el AGENTE asignado pasa a EN_PROGRESO o RESUELTO. `requerimientos.md` (RN-06, HU-04) y PLAN §7 decisión 2 dicen que solo el AGENTE cambia el estado. Se siguió ARCHITECTURE §6.3. **Pendiente:** actualizar `requerimientos.md` para que coincida.
- **AGENTE → CANCELADO:** el plan lo ubicaba como permitido; la implementación responde `403`, según ARCHITECTURE §6.3.
- **SOLICITANTE → CERRADO / CANCELADO:** `200` para el dueño del ticket (plan lo dejaba solo a AGENTE).
- **ADMIN:** `200` para cambios de estado.
- **Nombre de dependencia de pruebas:** ARCHITECTURE usa `spring-boot-starter-security-test`; el plan decía `oauth2-resource-server-test`. Se usó la de ARCHITECTURE.

## 3. Hallazgos del revisor (SEC-01 a SEC-11)

Revisión OWASP A01–A09 de solo lectura (P4, `.github/agents/revisor-seguridad-ms-tickets.agent.md`). Sin hallazgos CRITICAL ni HIGH en código. Decisión de Isaac: solo CRITICAL y HIGH se corrigen en P4, así que no hubo cambios de código por este paso.

| ID | Estado | Resumen | Motivo |
|---|---|---|---|
| SEC-01 | No corregido (verificado) | Secretos en `.env` local | Archivo sin versionar y listado en `.gitignore` (`ms-tickets/.gitignore:38`). Riesgo local, no en repositorio |
| SEC-02 | No corregido (verificado) | `TicketAccessPolicy.requireTransition` sin validación de rol | Riesgo medio. Aceptado y documentado; se revisará en la siguiente iteración |
| SEC-03 | No corregido | `/v3/api-docs` con `permitAll` | Motivo pendiente de confirmar con Isaac (no hay decisión registrada) |
| SEC-04 | **Pendiente de transcribir** | Detalle no guardado en el repositorio | Se necesita el texto original del revisor para completar esta fila |
| SEC-05 | No corregido | `CurrentUser` con múltiples roles | Motivo pendiente de confirmar con Isaac (no hay decisión registrada) |
| SEC-06 | No corregido | Valores por defecto en `application.yaml` | Solo en propiedades no secretas. Los secretos no tienen default (`JWT_SECRET`) |
| SEC-07 | No corregido | `Dockerfile` y `.dockerignore` vacíos | Fuera del alcance de P3 (contenedorización) |
| SEC-08 | No corregido | Sin auditoría de 401/403 | Mejora de observabilidad; fuera del alcance de P3 |
| SEC-09 | No corregido | `@Setter` en `Ticket` | Motivo pendiente de confirmar con Isaac (no hay decisión registrada) |
| SEC-10 | No corregido | `JwtProperties.toString` expone el secreto | Riesgo bajo; se registra como mejora |
| SEC-11 | **Pendiente de transcribir** | Detalle no guardado en el repositorio | Se necesita el texto original del revisor para completar esta fila |

## 4. Qué se corrigió

- Autenticación JWT como resource server (`JwtDecoderConfig`, `JwtRolesConverter`, handlers de 401/403, `SecurityConfig`, `CurrentUser`).
- Validación de `iss` y `aud`, algoritmo fijo HS256, rechazo de `none`, expiración.
- `JWT_SECRET` sin valor por defecto; arranque falla si mide menos de 32 bytes (`JwtDecoderConfig.java:28-34`).
- `server.error.include-stacktrace=never` e `include-message=never`.
- Clientes HTTP reales hacia ms-users y ms-catalog con `RestClient` (P5): timeouts configurables, propagación del Bearer token, mapeo de errores según ARCHITECTURE §3.4 y §7 (provider 404 → 422; 401/403/5xx/timeout/conexión → 503 `DEPENDENCY_UNAVAILABLE`).
- Excepciones de dominio con causa (`DomainException`, `BusinessRuleException`, `DependencyUnavailableException`), corrigiendo 6 violaciones de PMD `PreserveStackTrace`.
- Pruebas: 69 tests en total al cierre de P5, incluidos 19 tests de WireMock en `RemoteClientsTest`.

## 5. Qué no se corrigió y por qué

- SEC-01 a SEC-11 según la tabla de §3. Ninguno es CRITICAL ni HIGH.
- **Contract test contra `docs/contratos/ms-users.yaml`:** bloqueado. Requiere confirmación de Isaac y que `GET /api/v1/users/{id}` esté en `main` (Fernando). No se ejecutó ninguna prueba contra el ms-users real.
- **D13 (excepción):** ms-users no tiene cliente en ms-tickets. La validación de existencia de ticket se hace por la matriz de propiedad y por el catálogo; cualquier consulta a ms-users queda fuera de este paso hasta la confirmación.

## 6. Riesgos aceptados

- **Clave HS256 compartida** entre ms-users (emisor) y ms-tickets (validador). Decisión D5 del plan.
- **Access token válido hasta 15 minutos** después de desactivar un usuario, porque el JWT no se revoca en tiempo real.
- **Sin rate limiting en `/auth/login`.** Es responsabilidad de ms-users; no se modifica desde ms-tickets.

## 7. Evidencia de verificación

- `./mvnw -B verify` (con `NVD_API_KEY` cargada desde `.env` en un solo proceso, sin imprimirla): resultado registrado en `BITACORA-IA.md` entrada 20.
- Cierre previo de P5: BUILD SUCCESS, 69 tests, Checkstyle 0, PMD 0, JaCoCo cumplido, SpotBugs y OWASP Dependency-Check sin hallazgos.

## 8. Pendientes

1. Transcribir SEC-04 y SEC-11 desde la salida original del revisor.
2. Confirmar con Fernando que `GET /api/v1/users/{id}` está en `main` y autorizar la prueba de contrato contra ms-users (STOP).
3. Actualizar `docs/requerimientos.md` (RN-06, HU-04) para que coincida con ARCHITECTURE §6.3.
4. Decidir qué hacer con la etiqueta existente `p3-entrega-isaac` (mover o usar otro nombre).
5. Revisar si el `SpotBugs` exclude de `SPRING_CSRF_PROTECTION_DISABLED` (`spotbugs-exclude.xml`) puede estrecharse.
