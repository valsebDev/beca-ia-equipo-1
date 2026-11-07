# Prompt 03 — CRUD REST por capas (ms-catalog)

## ROL
Actúa como desarrollador backend senior especializado en Spring Boot 4 con arquitectura MVC por capas, Spring Data JPA, Jakarta Validation y RFC 9457 (`ProblemDetail`).

## CONTEXTO
Microservicio **ms-catalog** (Java 17, Spring Boot 4.1.2, PostgreSQL 16 con Flyway). Paquete base `com.ids.ms_catalog`.
Fuentes: `ARCHITECTURE.md` §3.2 (contrato de API), §3.4 (errores), §5.2 (estructura de paquetes), §5.3 (convenciones).
Historias: `docs/requerimientos.md` (HU-CAT-01 a HU-CAT-07). Esquema: `V1__create_catalog_tables.sql`.
Práctica 1: sin seguridad activa; no añadas `SecurityFilterChain`.

## TAREA
Implementa el CRUD de categorías (con borrado lógico) y de prioridades con su SLA:
1. `config/`: `@ConfigurationProperties` como record (`SlaProperties`, prefijo `app.sla`) y `OpenApiConfig`. `application.yaml` con `${ENV:default}`, `ddl-auto=validate`, `open-in-view=false` y Actuator con `health` únicamente.
2. `model/`: entidades JPA con solo `@Getter`, `@Setter` y `@NoArgsConstructor`.
3. `repository/`: interfaces Spring Data con consultas parametrizadas.
4. `dto/request/` y `dto/response/`: records con validaciones Jakarta.
5. `mapper/`: clases con métodos estáticos. Las entidades no salen del service.
6. `service/`: lógica con `@Transactional`. Valida unicidad, existencia y el orden y límite de horas del SLA.
7. `controller/`: prefijo `/api/v1`, `@Valid`, sin lógica de negocio.
8. `exception/`: excepciones de dominio y `GlobalExceptionHandler` (`@RestControllerAdvice`) que devuelve `ProblemDetail` con `type`, `title`, `status`, `detail` (español), `instance`, `code`, `timestamp` y `errors` en 400.

## RESTRICCIONES
- Código, clases, tablas y rutas en inglés. `detail` y valores de dominio en español.
- Prohibido: exponer entidades JPA, `@Autowired` en campos (usa `@RequiredArgsConstructor`), valores en duro (usa `app.sla.*`), `@Data` en entidades.
- Máximo 30 líneas por método y complejidad ciclomática ≤ 10.
- Un error 500 nunca devuelve el mensaje ni el stack trace; se registra con `log.error`.
- No agregues dependencias fuera del plan. No modifiques archivos fuera de `ms-catalog/`.

## FORMATO DE SALIDA
1. Plan breve con los archivos a crear o modificar.
2. Archivos completos, agrupados por paquete y en el orden de la TAREA.
3. Tabla resumen: `Archivo | Responsabilidad | HU o criterio que cubre`.

## EJEMPLO
Mapeo de error de negocio a `ProblemDetail`:
```json
{
  "type": "https://mesa-ayuda.ids/problems/category-already-exists",
  "title": "Conflicto con el estado actual",
  "status": 409,
  "detail": "Ya existe una categoría llamada \"Software\"",
  "instance": "/api/v1/categories",
  "code": "CATEGORY_ALREADY_EXISTS",
  "timestamp": "2026-11-05T07:04:35Z"
}
```

## VERIFICACIÓN
- [ ] `./mvnw -B -DskipTests compile` termina sin errores.
- [ ] `POST /api/v1/categories` devuelve 201 con `Location`; el nombre repetido devuelve 409.
- [ ] `DELETE /api/v1/categories/{id}` devuelve 204 y el registro sigue existiendo con `active=false`.
- [ ] `PUT /api/v1/priorities/{id}/sla` devuelve 400 con `errors` cuando `resolutionHours < responseHours`, y 404 si la prioridad no existe.
- [ ] Ninguna respuesta expone entidades JPA ni stack traces.
