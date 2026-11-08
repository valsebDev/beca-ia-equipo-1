# Prompt 01 — Requerimientos e historias de usuario (ms-catalog)

## ROL
Actúa como analista de requerimientos y Arquitecto de Software experto en ingeniería de requerimientos, historias de usuario (formato Mike Cohn), criterios de aceptación Gherkin y requerimientos no funcionales medibles.

## CONTEXTO
Proyecto: Mesa de Ayuda, monorepo `beca-ia-equipo-1`. Microservicio: **ms-catalog** (catálogo de servicio: categorías, prioridades y políticas SLA). Base de datos `catalog_db` (PostgreSQL 16).
Fuentes de verdad: `ARCHITECTURE.md` §0 (decisiones D1–D12), §2.2 (modelo de catálogo), §3.2 (contrato de API de ms-catalog), §3.4 (errores ProblemDetail) y §6.3 (matriz de autorización).
Alcance: Práctica 1, sin autenticación todavía (JWT llega en la Práctica 3).
Actores: ADMIN, SOLICITANTE, AGENTE y ms-tickets (consumidor futuro).

## TAREA
Genera `docs/requerimientos.md` con:
1. Un texto de negocio de 5 a 8 líneas desde la perspectiva del cliente.
2. Actores, entidades y reglas de negocio. Cada regla deducida que no esté explícita en las fuentes se marca como `SUPUESTO`.
3. Mínimo 5 historias de usuario (HU-01 a HU-05 o más) con el formato `Como <rol> quiero <acción> para <beneficio>`, y una tabla resumen con ID, Título, Prioridad MoSCoW y Estimación en puntos.
4. Criterios de aceptación en Gherkin (`Dado / Cuando / Entonces`) para cada HU, con al menos 1 escenario negativo por HU.
5. Mínimo 3 requerimientos no funcionales medibles y una sección de preguntas abiertas para el cliente.

## RESTRICCIONES
- Los IDs de HU (`HU-xx`) son estables: se referencian después en los comentarios `-- justifica:` de las migraciones SQL.
- No inventes endpoints fuera del contrato de ARCHITECTURE §3.2 sin marcarlos como `SUPUESTO`.
- Las reglas que dependan de otro microservicio se documentan, pero su implementación se difiere a la Práctica 3.
- Texto en español. Nombres de endpoints, campos y códigos de error tal como aparecen en el contrato.
- No modifiques archivos fuera de `ms-catalog/`.

## FORMATO DE SALIDA
Un único archivo Markdown con estas secciones en este orden:
1. Texto de negocio
2. Actores, entidades y reglas de negocio (tabla de reglas con columna "Origen")
3. Historias de usuario (tabla resumen + HU con Gherkin)
4. Requerimientos no funcionales (tabla con ID, atributo, requerimiento medible y verificación)
5. Preguntas abiertas

## EJEMPLO
Historia con escenario negativo:
```gherkin
Escenario negativo: Rechazar un nombre duplicado
  Dado que ya existe una categoría llamada "Software"
  Cuando envío POST /api/v1/categories con name "Software"
  Entonces recibo 409 con code CATEGORY_ALREADY_EXISTS y no se crea ningún registro
```

## VERIFICACIÓN
Antes de terminar, comprueba que:
- [ ] Hay 5 o más HU, cada una con formato "Como … quiero … para …".
- [ ] Cada HU tiene al menos un escenario negativo en Gherkin.
- [ ] La tabla resumen tiene ID, Título, Prioridad MoSCoW y Estimación para cada HU.
- [ ] Hay 3 o más RNF con métrica y forma de verificarlos.
- [ ] Toda regla no presente en ARCHITECTURE está marcada como `SUPUESTO`.
- [ ] Hay preguntas abiertas para el cliente.
