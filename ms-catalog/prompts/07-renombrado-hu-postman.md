# Prompt 07 — Renombrado de HU en Postman y requerimientos (ms-catalog)

## ROL
Actúa como analista de requerimientos que mantiene la consistencia de identificadores entre documentación y pruebas.

## CONTEXTO
Microservicio **ms-catalog**. La convención del equipo está en `docs/backlog.md`: los IDs de HU siguen el formato `HU-<SVC>-NN` con `SVC = CAT` para ms-catalog.
Archivos afectados: `ms-catalog/docs/requerimientos.md`, `ms-catalog/postman/ms-catalog.postman_collection.json`, `ms-catalog/prompts/02-modelo-datos-flyway.md`, `03-crud-capas-rest.md` y `04-pruebas-unitarias-web.md`.

## TAREA
1. Busca en los archivos afectados todas las referencias a HU-01 a HU-07 con formato antiguo.
2. Reemplázalas por `HU-CAT-01` a `HU-CAT-07` según la numeración de `ms-catalog/docs/requerimientos.md`, que es la fuente de verdad.
3. Separa el registro de consulta de prioridades en historias distintas, si la numeración de `requerimientos.md` lo requiere.
4. Renombra los títulos de carpetas y requests de Postman con el mismo ID, sin cambiar rutas ni aserciones.
5. Informa qué archivos cambiaron y cuántas líneas.

## RESTRICCIONES
- Una sola nomenclatura por documento: no mezcles `HU-01` con `HU-CAT-01`.
- No cambies criterios Gherkin ni códigos de respuesta esperados; solo identificadores.
- No modifies archivos fuera de `ms-catalog/`.

## FORMATO DE SALIDA
1. Tabla de equivalencias: `ID anterior | ID nuevo | Archivos`.
2. Lista de archivos modificados con el número de líneas cambiadas.
3. Búsqueda final de `HU-0` en los archivos afectados, con resultado vacío.

## VERIFICACIÓN
- [ ] Búsqueda de `HU-0[1-7]` (sin `CAT`) en los archivos afectados no devuelve resultados.
- [ ] El JSON de Postman sigue siendo válido.
- [ ] Los IDs de Postman coinciden con los de `requerimientos.md`.
