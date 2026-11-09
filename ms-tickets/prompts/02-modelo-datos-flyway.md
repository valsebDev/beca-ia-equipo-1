# Prompt 02: Modelo de datos y migraciones Flyway

## ROL
Eres un arquitecto de datos experto en PostgreSQL y Flyway, con criterio para integridad referencial y auditoría.

## CONTEXTO
Microservicio `ms-tickets` con PostgreSQL 16 (base `tickets_db`) y Flyway. Las migraciones viven en `src/main/resources/db/migration`. Los requerimientos están en `docs/requerimientos.md` y el modelo en `ARCHITECTURE.md`. El historial de tickets es append-only y el ticket usa control de concurrencia optimista (`version`).

## TAREA
Diseña y escribe la migración `V1__create_ticket_tables.sql` que cree:
1. Tabla `tickets` con id, título, descripción, estado, IDs de categoría, prioridad y solicitante/asignado, snapshot de SLA, fechas de SLA y de ciclo de vida, `version`, `created_at` y `updated_at`.
2. Tabla `ticket_history` con evento, estado origen/destino, asignado origen/destino, autor del cambio, comentario y `changed_at`, con FK a `tickets`.
3. Restricciones CHECK para estados y roles válidos, e índices para consultas por solicitante, asignado y estado.

## RESTRICCIONES
- Sintaxis PostgreSQL 16; nombres de tablas y columnas en inglés y en `snake_case`.
- No uses `ddl-auto` ni modifiques migraciones ya aplicadas; cualquier cambio posterior va en `V2__...`.
- No crees claves foráneas hacia `ms-users` ni `ms-catalog`: son microservicios independientes, solo se guardan IDs.
- Sin datos de prueba dentro de la migración.

## FORMATO DE SALIDA
Un único bloque ```sql con la migración comentada en una línea por sección, seguido de una tabla `| Tabla | Columna | Tipo | Restricción |` con las columnas clave.

## EJEMPLO
```sql
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_tickets_status CHECK (status IN ('ABIERTO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO','CANCELADO')),
    version BIGINT NOT NULL DEFAULT 0
);
```

## VERIFICACIÓN
- [ ] `V1__create_ticket_tables.sql` ejecuta sin errores sobre una base PostgreSQL 16 vacía.
- [ ] Los CHECK coinciden con los enums `TicketStatus`, `HistoryEventType` y `UserRole`.
- [ ] `ddl-auto: validate` no reporta diferencias con las entidades JPA.
- [ ] No hay FK hacia otros microservicios.
