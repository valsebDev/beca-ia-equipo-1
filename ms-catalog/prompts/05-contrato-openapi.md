# Prompt 05 — Contrato OpenAPI de ms-catalog

## ROL
Actúa como arquitecto de APIs que documenta contratos REST con OpenAPI 3.0.3 y conoce RFC 9457 (`ProblemDetail`).

## CONTEXTO
Microservicio **ms-catalog** (categorías, prioridades y SLA) de la Mesa de Ayuda. Práctica 1, sin autenticación.
Fuentes: `ARCHITECTURE.md` §3.2 (contrato de API), §3.4 (errores), `ms-catalog/docs/requerimientos.md` (HU-CAT-01 a HU-CAT-07) y `docs/backlog.md` (convención de IDs `HU-<SVC>-NN`).

## TAREA
Genera `docs/contratos/ms-catalog.yaml` con OpenAPI 3.0.3 que cubra todos los endpoints de `/api/v1`:
1. `info`, `servers` y `tags` separados para categorías y prioridades.
2. Un path por endpoint de categorías (listar, listar activas, consultar por id, crear, editar, desactivar) y de prioridades (listar, editar SLA).
3. Esquemas de request y response como `components/schemas`, sin exponer entidades JPA.
4. Respuestas de error con `ProblemDetail` (`400` con `errors`, `404`, `409`, `405`) referenciadas desde `components/responses`.
5. Cada operación indica la HU-CAT que cubre en su `description`.

## RESTRICCIONES
- Nombres de campos y rutas en inglés; `description` y ejemplos en español.
- Los patrones de `code` deben reflejar el dominio real: categorías únicas sin distinguir mayúsculas, prioridades con 4 códigos.
- Las horas de SLA se validan en el rango definido en `ARCHITECTURE.md`; no inventes límites distintos.
- No definas esquemas de seguridad: la Práctica 1 no tiene autenticación. Documenta esa ausencia como nota, no como error.
- No modifiques archivos fuera de `docs/contratos/`.

## FORMATO DE SALIDA
1. Lista de paths con método, HU cubierta y código de respuesta principal.
2. Archivo YAML completo.
3. Tabla resumen: `Path | Método | HU | Respuestas documentadas`.

## VERIFICACIÓN
- [ ] `npx @redocly/cli lint docs/contratos/ms-catalog.yaml` sin errores de sintaxis de OpenAPI.
- [ ] Cada endpoint de `/api/v1` de ms-catalog aparece una sola vez.
- [ ] Ningún error devuelve un esquema distinto de `ProblemDetail`.
