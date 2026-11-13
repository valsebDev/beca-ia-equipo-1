# Informe de seguridad: ms-catalog (Práctica 3)

Autor: Sebastián (ms-catalog). Referencias: [ARCHITECTURE.md](../../ARCHITECTURE.md) (§3.2, §6, §7, D13), [docs/seguridad.md](../../docs/seguridad.md) y [plan](planes/PLAN-SEGURIDAD-MS-CATALOG-P3.md).

## 1. Matriz endpoint × rol

Implementada en `SecurityConfig` con `requestMatchers(...).hasRole("ADMIN")` y `anyRequest().authenticated()`. No se usa `@PreAuthorize`.

| Endpoint | Sin token | SOLICITANTE | AGENTE | ADMIN | Pruebas |
|---|---|---|---|---|---|
| GET `/api/v1/categories`, `/{id}` | 401 | 200 | 200 | 200 | `CategoryControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `lecturaPermitidaParaTodosLosRoles`, `consultaCategoriaPorIdPermitidaParaTodosLosRoles` |
| POST `/api/v1/categories` | 401 | 403 | 403 | 201 | `CategoryControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `crearCategoriaRequiereAdmin`, `CategoryControllerTest.createDevuelve201ConCabeceraLocation` |
| PUT `/api/v1/categories/{id}` | 401 | 403 | 403 | 200 | `CategoryControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `actualizarCategoriaRequiereAdmin`, `adminPuedeActualizarCategoria` |
| DELETE `/api/v1/categories/{id}` | 401 | 403 | 403 | 204 | `CategoryControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `eliminarCategoriaRequiereAdmin`, `adminPuedeEliminarCategoria` |
| GET `/api/v1/priorities`, `/{id}` | 401 | 200 | 200 | 200 | `PriorityControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `lecturaPermitidaParaTodosLosRoles`, `consultaPrioridadPorIdPermitidaParaTodosLosRoles` |
| POST `/api/v1/priorities` | 401 | 403 | 403 | 201 | `PriorityControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `crearPrioridadRequiereAdmin`, `PriorityControllerTest.createDevuelve201ConCabeceraLocation` |
| PUT `/api/v1/priorities/{id}/sla` | 401 | 403 | 403 | 200 | `PriorityControllerSecurityTest.todaRutaProtegidaSinTokenDevuelve401`, `actualizarSlaRequiereAdmin`, `PriorityControllerTest.updateSlaDevuelve200ConLasNuevasHoras` |
| GET `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**` | 200 | 200 | 200 | 200 | `ActuatorSecurityTest.healthEsPublico` |
| Otras rutas de `/actuator/*` | 401 | 401 | 401 | 401 | `ActuatorSecurityTest.otrosEndpointsDeActuatorRequierenToken` |

Además, en POST/PUT/DELETE el controller llama a `AdminGuard` (D13) antes del service. Si el ADMIN está desactivado en ms-users o no existe, responde 403; si ms-users no responde correctamente, responde 503.

## 2. Qué corregí del plan

| Plan original | Lo aplicado | Motivo |
|---|---|---|
| `@PreAuthorize` en controllers | Reglas por ruta en `SecurityConfig` | Centraliza la matriz en un único lugar. |
| Fase B diferida (ADMIN activo) | Implementada: `UsersClient`, `AdminGuard`, `BearerTokenRelayInterceptor` | Aprobado por el equipo; se implementó como D13. |
| Pruebas de cliente con `MockRestServiceServer` o WireMock | WireMock (`wiremock-standalone` 3.13.2, scope test) | Aprobado por el usuario. |
| Contrato de ms-users sin `GET /api/v1/users/{id}` | Simulación alineada con el contrato actualizado (`id`, `active`) | Pendiente de integración real (ver §5). |

## 3. Hallazgos del revisor de seguridad

Revisión realizada con el agente de solo lectura [revisor-seguridad-ms-catalog](../../.github/agents/revisor-seguridad-ms-catalog.agent.md), en una sesión separada de la implementación.

| ID | Severidad | OWASP | Archivo | Estado |
|---|---|---|---|---|
| R-1 | MEDIUM | A01 | `CategoryController`, `PriorityController` | Corregido: `AdminGuard` valida ADMIN activo en ms-users antes de cada mutación (D13). Verificado con pruebas unitarias y WireMock. |
| R-2 | LOW | A02 | `SecurityTokenValidationTest` | Corregido: la clave de firma inválida se genera aleatoriamente en el test; ya no hay un secreto literal alterno. |
| R-3 | LOW | A05 | `docs/contratos/ms-catalog.yaml` | Corregido: el contrato declara `bearerAuth` y las respuestas 401/403 de las rutas protegidas. |

No hubo hallazgos CRÍTICOS ni ALTOS.

## 4. Qué decidí no corregir y por qué

- **CSRF deshabilitado** (`SPRING_CSRF_PROTECTION_DISABLED`, SpotBugs): es una API stateless; el JWT viaja en `Authorization` y no hay cookies ni sesión. El equipo lo aceptó como falso positivo y la excepción tiene justificación en `SecurityConfig`.
- **Swagger y `/v3/api-docs` públicos** (§6.2): se controla con `SWAGGER_ENABLED`; en producción debe valer `false`.
- **Sin rate limiting ni CORS**: fuera del alcance de D11 y §6.2.

## 5. Riesgos aceptados y pendientes

| Riesgo | Tratamiento |
|---|---|
| Llave HS256 compartida entre servicios (`JWT_SECRET`) | Aceptado (D5). Cualquier servicio con la llave puede emitir tokens. |
| Access token vigente hasta 15 min tras desactivar una cuenta | Aceptado. D13 cierra el riesgo solo en las mutaciones de ms-catalog; los GET siguen con el token vigente. |
| Sin rate limiting en `/auth/login` (ms-users) | Aceptado (D11). Responsabilidad de ms-users. |
| Integración real con ms-users no probada | Pendiente. La simulación WireMock sigue el contrato actualizado; el código de ms-users aún devuelve `userIdentifier`/`isActive`, por lo que la prueba real debe hacerse tras fusionar su código. |
| Equivalencia entre `sub` del JWT y `id` de ms-users | Pendiente de confirmar con el equipo de ms-users. |

## 6. Evidencia

- Matriz y casos 401/403/200: `CategoryControllerSecurityTest`, `PriorityControllerSecurityTest`, `SecurityTokenValidationTest`, `ActuatorSecurityTest`.
- Guard D13: `AdminGuardTest` (7 pruebas) y `UsersClientWireMockTest` (8 pruebas: 200, inactivo, 404, 401, 500, conexión cerrada, timeout y `Authorization`).
- Última ejecución de `./mvnw -B verify`: 120 pruebas sin fallos; Checkstyle, PMD/CPD, SpotBugs  y JaCoCo pasaron. `BUILD SUCCESS`.
