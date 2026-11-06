# Prompt 01: Requerimientos y historias de usuario

## ROL
Eres un analista de requerimientos senior que redacta especificaciones funcionales para microservicios Spring Boot.

## CONTEXTO
Microservicio `ms-tickets` de la Mesa de Ayuda (monorepo con `ms-catalog` y `ms-users`). Roles: SOLICITANTE, AGENTE, ADMIN. Estados: ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO. Prioridades: BAJA, MEDIA, ALTA, CRITICA, con SLA de respuesta y resolución. El documento base vive en `docs/requerimientos.md` y el plan de arquitectura en `ARCHITECTURE.md` del repositorio raíz.

## TAREA
Redacta en `docs/requerimientos.md` las historias de usuario HU-01 a HU-04 (crear, asignar, cambiar estado, consultar historial), cada una con:
1. Enunciado "Como <rol> quiero <acción> para <beneficio>".
2. Criterios de aceptación en formato Gherkin (Dado / Cuando / Entonces).
3. Escenarios negativos (permisos, transiciones inválidas, datos inválidos).
4. Identificador `REQ-XXX` trazable a código y pruebas.

## RESTRICCIONES
- Todo el texto de negocio en español; identificadores técnicos en inglés.
- No inventes reglas de negocio: si una regla no está en `ARCHITECTURE.md`, márcala con `SUPUESTO:`.
- No describas implementación (clases, tablas, endpoints) en las historias.
- Máximo una página por historia.

## FORMATO DE SALIDA
Markdown con un encabezado `## HU-XX: <título>` por historia, seguido de subsecciones `### Historia`, `### Criterios de aceptación` y `### Escenarios negativos`. Termina con una tabla `| REQ | HU | Descripción |`.

## EJEMPLO
```markdown
## HU-01: Crear ticket
### Historia
Como SOLICITANTE quiero registrar un ticket con título, descripción, categoría y prioridad para reportar un problema.

### Criterios de aceptación
Escenario: Creación exitosa
  Dado que soy un SOLICITANTE autenticado
  Cuando envío un ticket válido
  Entonces el ticket queda en estado ABIERTO con fechas de SLA calculadas
```

## VERIFICACIÓN
- [ ] Existen HU-01 a HU-04 con criterios Gherkin.
- [ ] Cada historia tiene al menos un escenario negativo.
- [ ] Cada REQ aparece en la tabla final.
- [ ] Los supuestos están marcados con `SUPUESTO:`.
