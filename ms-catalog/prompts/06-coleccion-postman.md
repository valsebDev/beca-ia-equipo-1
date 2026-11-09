# Prompt 06 — Colección de Postman de ms-catalog

## ROL
Actúa como QA de APIs REST que diseña colecciones de Postman con pruebas positivas y negativas por historia de usuario.

## CONTEXTO
Microservicio **ms-catalog** en `{{baseUrl}}` (por defecto `http://localhost:8081`). Práctica 1, sin autenticación.
Fuentes: `docs/contratos/ms-catalog.yaml` (contrato OpenAPI), `ms-catalog/docs/requerimientos.md` (HU-CAT-01 a HU-CAT-07) y `docs/backlog.md` (convención de IDs `HU-<SVC>-NN`).

## TAREA
Genera `postman/ms-catalog.postman_collection.json` (formato Collection v2.1) con:
1. Una carpeta de salud (`/actuator/health`).
2. Una carpeta por bloque de HU: "Categorías (HU-CAT-01 a HU-CAT-04)" y "Prioridades y SLA (HU-CAT-05 a HU-CAT-07)".
3. Cada request nombrado con su HU: `HU-CAT-0X <acción> -> <código>`.
4. Por cada HU, al menos un escenario positivo y un escenario negativo (`[Negativo]`): 404, 409 o 400 según corresponda.
5. Scripts de prueba (`pm.test`) que validen código de estado y forma del `ProblemDetail` en los errores.
6. Variables de colección (`baseUrl`, `categoryId`, `priorityId`) que se guardan desde la respuesta de creación.

## RESTRICCIONES
- Los nombres de HU deben coincidir exactamente con `ms-catalog/docs/requerimientos.md`.
- Las rutas deben coincidir con el contrato OpenAPI, sin inventar endpoints.
- Usa `{{baseUrl}}` en todas las URLs; no pongas hosts en duro.
- No incluyas credenciales ni tokens.
- No modifiques archivos fuera de `ms-catalog/postman/`.

## FORMATO DE SALIDA
1. Estructura de carpetas con el número de requests por carpeta.
2. JSON completo de la colección.
3. Tabla resumen: `Request | HU | Tipo (positivo/negativo) | Código esperado`.

## VERIFICACIÓN
- [ ] El JSON es válido (`python3 -m json.tool`).
- [ ] Cada HU de `requerimientos.md` tiene al menos un request positivo y uno negativo.
- [ ] `newman run postman/ms-catalog.postman_collection.json` pasa con la app y PostgreSQL levantados.
