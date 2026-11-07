# Prompt 02 — Modelo de datos y migraciones Flyway (ms-catalog)

## ROL
Actúa como arquitecto de datos y desarrollador backend senior, experto en PostgreSQL 16, Flyway, Spring Data JPA y modelado relacional con restricciones explícitas.

## CONTEXTO
Microservicio **ms-catalog** (`catalog_db`, PostgreSQL 16) con Spring Boot 4.1.2 y Java 17. Fuente de verdad: `ARCHITECTURE.md` §2.2 (modelo ER de catálogo), §2.4 (estrategia Flyway) y §0 (D3, D10).
Requerimientos y IDs de historia: `docs/requerimientos.md` (HU-CAT-01 a HU-CAT-07).
Dependencias del `pom.xml`: `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `flyway-database-postgresql`, `postgresql`.

## TAREA
1. Verifica el `pom.xml`: Java 17, Spring Boot 4.1.2 y las dependencias de la lista. Quita dependencias no justificadas por el plan.
2. Genera `src/main/resources/db/migration/V1__create_catalog_tables.sql` con las tablas `categories`, `priorities` y `sla_policies` según el ER de §2.2.
3. Genera `V2__seed_priorities_and_sla.sql` con las 4 prioridades (`CRITICA`, `ALTA`, `MEDIA`, `BAJA`), sus SLA (CRITICA 1h/4h, ALTA 4h/24h, MEDIA 8h/48h, BAJA 24h/120h) y 3 categorías iniciales.

## RESTRICCIONES
- Tablas y columnas en inglés `snake_case` plural; PK `BIGSERIAL`; fechas `TIMESTAMPTZ`; enums como `VARCHAR` + `CHECK`.
- Cada `CREATE INDEX` y cada restricción `UNIQUE` lleva encima un comentario `-- justifica: HU-xx <consulta que optimiza o regla que protege>`.
- Solo SQL portable de PostgreSQL 16. Sin datos sensibles. Las migraciones no se modifican una vez aplicadas: los cambios van en una nueva `V{n+1}`.
- No agregues `@Data` ni dependencias fuera del plan.
- Los valores de dominio (`CRITICA`, `ALTA`, `MEDIA`, `BAJA`) se mantienen en español.

## FORMATO DE SALIDA
1. Lista breve de cambios en `pom.xml`, con la razón de cada uno.
2. `V1__create_catalog_tables.sql` completo.
3. `V2__seed_priorities_and_sla.sql` completo.
4. Tabla de justificaciones: `Índice o restricción | HU | Consulta o regla que cubre`.

## EJEMPLO
Índice justificado en Flyway:
```sql
-- justifica: HU-CAT-02 y HU-CAT-03 rechazan nombres duplicados al crear y al renombrar una categoría (consulta de unicidad por name)
CONSTRAINT uk_categories_name UNIQUE (name)
```

## VERIFICACIÓN
- [ ] `./mvnw -B -DskipTests compile` termina sin errores.
- [ ] Cada índice y restricción `UNIQUE` tiene su comentario `-- justifica: HU-xx`.
- [ ] La aplicación arranca contra PostgreSQL 16 con `ddl-auto=validate`, y Flyway reporta "Successfully applied 2 migrations".
- [ ] Las prioridades quedan en el orden `CRITICA` (nivel 1) a `BAJA` (nivel 4) y cada una tiene exactamente un SLA.
