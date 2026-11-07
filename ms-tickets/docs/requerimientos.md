# Requerimientos — ms-tickets (Mesa de Ayuda)

## 1. Texto de negocio

La mesa de ayuda necesita un canal único para que el personal reporte incidencias de soporte (equipos, accesos, software). Cada solicitud debe quedar registrada con título, descripción, categoría y prioridad, para que nadie pierda seguimiento. El sistema debe calcular automáticamente el plazo de primera respuesta y de resolución según la prioridad, para que el equipo de soporte sepa qué atender primero. Los agentes deben ver solo el trabajo que se les asigna y poder avanzar cada ticket por su ciclo de vida. El administrador asigna los tickets a agentes y necesita ver quién hizo cada cambio y cuándo. Toda modificación de un ticket debe conservar un historial que no pueda alterarse, para auditar la atención brindada.

## 2. Actores, entidades y reglas de negocio

### 2.1 Actores

| Actor | Descripción |
|---|---|
| SOLICITANTE | Usuario que reporta incidencias; ve y crea solo sus tickets. |
| AGENTE | Personal de soporte; ve los tickets asignados y cambia su estado. |
| ADMIN | Administrador; asigna tickets a agentes y consulta todos los tickets. |
| ms-users | Sistema externo que valida la existencia y rol de los agentes. |
| ms-catalog | Sistema externo que provee categorías, prioridades y políticas SLA. |

### 2.2 Entidades

- **Ticket**: id, título, descripción, estado, categoría, prioridad, snapshot de SLA, fechas límite, solicitante, agente asignado, fechas de primera respuesta, resolución y cierre, versión.
- **TicketHistory**: id, ticket, tipo de evento, estado anterior y nuevo, agente anterior y nuevo, usuario que realizó el cambio, comentario, fecha del cambio.
- **Categoría, Prioridad y Política SLA**: pertenecen a ms-catalog; ms-tickets solo guarda referencias (IDs) y una copia del SLA.

### 2.3 Reglas de negocio

- RN-01: Un ticket nace en estado `ABIERTO` y solo puede ser creado por un `SOLICITANTE`.
- RN-02: Un `SOLICITANTE` solo puede consultar sus propios tickets.
- RN-03: Solo un `ADMIN` puede asignar un ticket; el asignado debe existir en ms-users, estar activo y tener rol `AGENTE`.
- RN-04: La asignación solo es válida si el ticket está en `ABIERTO` o `ASIGNADO`; pasa a `ASIGNADO`.
- RN-05: Transiciones de estado permitidas: `ASIGNADO` → `EN_PROGRESO` → `RESUELTO` → `CERRADO`. `CANCELADO` es posible desde `ABIERTO` o `ASIGNADO`. Cualquier otra transición se rechaza.
- RN-06: Solo el `AGENTE` asignado puede cambiar el estado de un ticket (`ADMIN` asigna, no ejecuta trabajo). SUPUESTO.
- RN-07: La categoría y la prioridad deben existir y estar activas en ms-catalog al crear el ticket.
- RN-08: Al crear el ticket se copian las horas de SLA de la prioridad y se calculan `responseDueAt` y `resolutionDueAt` a partir de la fecha de creación.
- RN-09: Un ticket está `overdue` cuando la hora actual supera `resolutionDueAt` y su estado no es `RESUELTO`, `CERRADO` ni `CANCELADO`.
- RN-10: Cada creación, asignación y cambio de estado genera una fila en `TicketHistory` en la misma transacción. SUPUESTO sobre el contenido mínimo del comentario: opcional, máximo 1000 caracteres.
- RN-11: El historial es append-only; no se modifica ni se elimina. Los tickets no se borran, se cancelan.
- RN-12: Si dos usuarios modifican el mismo ticket a la vez, el segundo cambio se rechaza (conflicto de concurrencia). SUPUESTO sobre el criterio de conflicto.
- RN-13: Si ms-users o ms-catalog no responden, la operación que los necesita se rechaza con error de dependencia no disponible (SUPUESTO sobre la política de reintento: ninguna).
- RN-14: `RESUELTO` registra `resolvedAt`; `CERRADO` registra `closedAt`. SUPUESTO de que ambos quedan en nulo hasta alcanzar el estado correspondiente.
- RN-15: SUPUESTO temporal: la identidad llega por las cabeceras X-User-Id / X-User-Role hasta implementar JWT en P3

## 3. Historias de usuario

| ID | Título | Prioridad MoSCoW | Estimación (pts) |
|---|---|---|---|
| HU-01 | Crear ticket de soporte | Must | 5 |
| HU-02 | Consultar mis tickets y su detalle | Must | 3 |
| HU-03 | Asignar ticket a un agente | Must | 5 |
| HU-04 | Cambiar estado del ticket asignado | Must | 5 |
| HU-05 | Consultar historial de un ticket | Should | 2 |

**HU-01 — Crear ticket de soporte.** Como SOLICITANTE quiero registrar una incidencia con título, descripción, categoría y prioridad para que el equipo de soporte la atienda con un plazo definido.

**HU-02 — Consultar mis tickets y su detalle.** Como SOLICITANTE quiero listar mis tickets y ver su detalle para dar seguimiento a mis incidencias.

**HU-03 — Asignar ticket a un agente.** Como ADMIN quiero asignar un ticket a un agente para que tenga un responsable claro.

**HU-04 — Cambiar estado del ticket asignado.** Como AGENTE quiero avanzar el estado de mis tickets para reflejar el progreso real de la atención.

**HU-05 — Consultar historial de un ticket.** Como ADMIN quiero consultar el historial de cambios de un ticket para auditar la atención brindada.

## 4. Criterios de aceptación (Gherkin)

### HU-01 — Crear ticket de soporte

```gherkin
Escenario: Creación exitosa de un ticket con SLA calculado
  Dado un SOLICITANTE autenticado
  Y existe la categoría "Hardware" activa y la prioridad "ALTA" activa con SLA de 4 y 24 horas
  Cuando envía un ticket con título válido, descripción válida, esa categoría y esa prioridad
  Entonces el sistema responde 201 con estado "ABIERTO"
  Y "responseDueAt" es la hora de creación más 4 horas
  Y "resolutionDueAt" es la hora de creación más 24 horas
  Y se registra un evento "CREATED" en el historial

Escenario negativo: Categoría inexistente en ms-catalog
  Dado un SOLICITANTE autenticado
  Cuando envía un ticket con un categoryId que ms-catalog responde 404
  Entonces el sistema responde 422 con código "CATEGORY_INVALID"
  Y no se guarda ningún ticket

Escenario negativo: Título demasiado corto
  Dado un SOLICITANTE autenticado
  Cuando envía un ticket con título de 3 caracteres
  Entonces el sistema responde 400 con código "VALIDATION_ERROR" indicando el campo "title"
```

### HU-02 — Consultar mis tickets y su detalle

```gherkin
Escenario: Listado de tickets propios
  Dado un SOLICITANTE autenticado con 2 tickets propios y 1 ajeno
  Cuando consulta GET /api/v1/tickets
  Entonces el sistema responde 200 con exactamente sus 2 tickets

Escenario: Detalle de ticket propio con indicador de vencimiento
  Dado un SOLICITANTE autenticado dueño del ticket 10
  Y el ticket 10 superó "resolutionDueAt" y está en "EN_PROGRESO"
  Cuando consulta GET /api/v1/tickets/10
  Entonces el sistema responde 200 con "overdue" igual a true

Escenario negativo: Detalle de ticket ajeno
  Dado un SOLICITANTE autenticado que no es dueño del ticket 11
  Cuando consulta GET /api/v1/tickets/11
  Entonces el sistema responde 403 con código "FORBIDDEN"

Escenario negativo: Ticket inexistente
  Dado un SOLICITANTE autenticado
  Cuando consulta GET /api/v1/tickets/999999
  Entonces el sistema responde 404 con código "TICKET_NOT_FOUND"
```

### HU-03 — Asignar ticket a un agente

```gherkin
Escenario: Asignación exitosa a un agente activo
  Dado un ADMIN autenticado
  Y un ticket 20 en estado "ABIERTO"
  Y un usuario 5 con rol "AGENTE" y activo en ms-users
  Cuando asigna el ticket 20 al usuario 5
  Entonces el sistema responde 200 con estado "ASIGNADO" y "assigneeId" igual a 5
  Y se registra un evento "ASSIGNED" en el historial

Escenario negativo: Asignar a un usuario que no es agente
  Dado un ADMIN autenticado y un ticket 20 en estado "ABIERTO"
  Cuando asigna el ticket 20 a un usuario con rol "SOLICITANTE"
  Entonces el sistema responde 422 con código "ASSIGNEE_INVALID"
  Y el ticket permanece en "ABIERTO"

Escenario negativo: Asignar un ticket ya resuelto
  Dado un ADMIN autenticado y un ticket 21 en estado "RESUELTO"
  Cuando intenta asignarlo a un agente válido
  Entonces el sistema responde 409 con código "TICKET_INVALID_TRANSITION"
```

### HU-04 — Cambiar estado del ticket asignado

```gherkin
Escenario: Avance de estado por el agente asignado
  Dado un AGENTE autenticado asignado al ticket 30 en estado "ASIGNADO"
  Cuando cambia el estado a "EN_PROGRESO"
  Entonces el sistema responde 200 con estado "EN_PROGRESO"
  Y se registra un evento "STATUS_CHANGED" en el historial

Escenario: Cancelación desde ABIERTO por el agente asignado
  Dado un AGENTE autenticado asignado al ticket 31 en estado "ASIGNADO"
  Cuando cambia el estado a "CANCELADO" con comentario "Duplicado"
  Entonces el sistema responde 200 con estado "CANCELADO" y "closedAt" nulo

Escenario negativo: Transición no permitida
  Dado un AGENTE autenticado asignado al ticket 32 en estado "ASIGNADO"
  Cuando cambia el estado a "RESUELTO"
  Entonces el sistema responde 409 con código "TICKET_INVALID_TRANSITION"

Escenario negativo: Agente no asignado
  Dado un AGENTE autenticado que no está asignado al ticket 33
  Cuando intenta cambiar su estado a "EN_PROGRESO"
  Entonces el sistema responde 403 con código "FORBIDDEN"
```

### HU-05 — Consultar historial de un ticket

```gherkin
Escenario: Historial ordenado cronológicamente
  Dado un ADMIN autenticado
  Y un ticket 40 con eventos CREATED, ASSIGNED y STATUS_CHANGED
  Cuando consulta GET /api/v1/tickets/40/history
  Entonces el sistema responde 200 con los 3 eventos en orden de "changedAt"

Escenario negativo: Historial de ticket inexistente
  Dado un ADMIN autenticado
  Cuando consulta GET /api/v1/tickets/999999/history
  Entonces el sistema responde 404 con código "TICKET_NOT_FOUND"

Escenario negativo: Solicitante ajeno consulta historial
  Dado un SOLICITANTE autenticado que no es dueño del ticket 41
  Cuando consulta GET /api/v1/tickets/41/history
  Entonces el sistema responde 403 con código "FORBIDDEN"
```

## 5. Requerimientos no funcionales

- RNF-01 (Rendimiento): el endpoint `POST /api/v1/tickets` responde en menos de 800 ms en p95 con 50 usuarios concurrentes, excluyendo latencia de red, medido con una prueba de carga en el entorno de Docker Compose.
- RNF-02 (Disponibilidad de dependencias): si ms-users o ms-catalog no responden, ms-tickets devuelve 503 en menos de 2 segundos (timeout de Feign configurado) y no pierde datos ya guardados.
- RNF-03 (Integridad y auditoría): el 100 % de las operaciones de creación, asignación y cambio de estado genera una fila en `ticket_history` en la misma transacción; se verifica con pruebas de servicio.
- RNF-04 (Seguridad de errores): ninguna respuesta de error expone stack traces ni mensajes internos; todas usan formato RFC 9457.
- RNF-05 (Mantenibilidad): métodos de máximo 30 líneas y complejidad ciclomática menor o igual a 10, verificado con el reporte de calidad.

## 6. Preguntas abiertas al cliente

1. ¿Quién puede cancelar un ticket: solo el agente asignado, el solicitante, el ADMIN o varios roles?
2. ¿El comentario de un cambio de estado es obligatorio para `RESUELTO` o `CANCELADO`?
3. ¿Los valores de SLA (horas de respuesta y resolución) son fijos o los ajusta el ADMIN por ticket?
4. ¿Qué ocurre si el agente asignado queda inactivo en ms-users mientras tiene tickets abiertos? ¿Se reasignan automáticamente?
5. ¿Se requiere notificación por correo al solicitante en cada cambio de estado?
6. ¿El solicitante puede editar título o descripción después de crear el ticket?
7. ¿Los tickets cerrados deben poder reabrirse?
