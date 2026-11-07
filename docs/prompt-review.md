## Prompt revisado: ms-tickets/prompts/01-requerimientos-hu.md (rama practica-1-isaac) (autor: Isaac · revisor: Sebastián)
| Pieza | ¿Estaba? | Mejora propuesta |
|---|---|---|
| Rol | Sí | — |
| Contexto | Sí | Referencié los archivos con `#file:` (guía, Práctica 1, sección 3) y declaré `ARCHITECTURE.md` como única fuente de reglas. |
| Tarea | Parcial | Faltaban los 3 RNF y las preguntas abiertas que pide la Práctica 1 (paso 1 y rúbrica "Requerimientos"). |
| Restricciones | Parcial | Agregué un escenario negativo mínimo por HU y la regla "si te falta información, pregúntame" (Anexo A). |
| Formato de salida | Sí | Agregué las secciones RNF y Preguntas abiertas, y una fila por REQ en la tabla final. |
| Ejemplo | Parcial | Agregué un escenario negativo al ejemplo de HU-01 (la guía exige negativos por HU). |
| Verificación | Sí | Hice los criterios comprobables: negativo por HU, 3 RNF, preguntas abiertas y supuestos marcados. |

### Antes
````markdown
# Prompt 01: Requerimientos y historias de usuario

## ROL
Eres un analista de requerimientos senior que redacta especificaciones funcionales para microservicios Spring Boot.

## CONTEXTO
Microservicio `ms-tickets` de la Mesa de Ayuda (monorepo con `ms-catalog` y `ms-users`). Roles: SOLICITANTE, AGENTE, ADMIN. Estados: ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO. Prioridades: BAJA, MEDIA, ALTA, CRITICA, con SLA de respuesta y resolución. El documento base vive en `docs/requerimientos.md` y el plan de arquitectura en `ARCHITECTURE.md` del repositorio raíz.

## TAREA
Redacta en `docs/requerimientos.md` las historias de usuario HU-01 a HU-05 (crear, asignar, cambiar estado, consultar historial), cada una con:
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
- [ ] Existen HU-01 a HU-05 con criterios Gherkin.
- [ ] Cada historia tiene al menos un escenario negativo.
- [ ] Cada REQ aparece en la tabla final.
- [ ] Los supuestos están marcados con `SUPUESTO:`.
````

### Después
````markdown
# Prompt 01: Requerimientos y historias de usuario

## ROL
Eres un analista de requerimientos senior que redacta especificaciones funcionales para microservicios Spring Boot.

## CONTEXTO
Proyecto: Mesa de Ayuda, microservicio `ms-tickets` (monorepo con `ms-catalog` y `ms-users`). Roles: SOLICITANTE, AGENTE, ADMIN. Estados: ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO. Prioridades: BAJA, MEDIA, ALTA, CRITICA, con SLA de respuesta y resolución.
Archivos relevantes: #file:ARCHITECTURE.md (única fuente de reglas de negocio) y #file:docs/requerimientos.md (documento que vas a redactar).

## TAREA
Redacta en #file:docs/requerimientos.md las historias de usuario HU-01 a HU-05 (crear, asignar, cambiar estado, consultar historial), y agrega:
1. Para cada HU: enunciado "Como <rol> quiero <acción> para <beneficio>" y criterios de aceptación en Gherkin (Dado / Cuando / Entonces).
2. Identificador `REQ-XXX` trazable a código y pruebas.
3. Tres requerimientos no funcionales (RNF) sobre seguridad, rendimiento y auditoría.
4. Lista de preguntas abiertas que harías al cliente.

## RESTRICCIONES
- Todo el texto de negocio en español; identificadores técnicos en inglés.
- No inventes reglas de negocio: si una regla no está en `ARCHITECTURE.md`, márcala con `SUPUESTO:`.
- No describas implementación (clases, tablas, endpoints) en las historias.
- Máximo una página por historia.
- Cada HU tiene al menos un escenario negativo en Gherkin (permisos, transición inválida o dato inválido), con el rechazo esperado.
- Si te falta información, pregúntame antes de suponer.

## FORMATO DE SALIDA
Markdown con un encabezado `## HU-XX: <título>` por historia, seguido de `### Historia`, `### Criterios de aceptación` y `### Escenarios negativos`. Después, las secciones `## RNF` y `## Preguntas abiertas`. Termina con una tabla `| REQ | HU | Descripción |`, con una fila por REQ y sin identificadores repetidos.

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

### Escenarios negativos
Escenario: Título vacío (negativo)
  Dado que soy un SOLICITANTE autenticado
  Cuando envío un ticket sin título
  Entonces el ticket no se crea y se informa el dato inválido
```

## VERIFICACIÓN
- [ ] Existen HU-01 a HU-05 con criterios Gherkin.
- [ ] Cada HU tiene al menos un escenario negativo.
- [ ] Hay 3 RNF y una lista de preguntas abiertas.
- [ ] Cada REQ aparece en la tabla final, sin repetir identificadores.
- [ ] Los supuestos están marcados con `SUPUESTO:`.
````

### Resultado al probarlo
- Prueba del autor (Isaac), tarea "generar HU-01 a HU-05 de ms-tickets": la respuesta con la versión mejorada incluyó HU-01 a HU-05, un escenario negativo por HU, las secciones RNF y Preguntas abiertas, y la tabla REQ. Con la versión original faltaban RNF y preguntas abiertas.
- Prueba del revisor (Sebastián), misma tarea y mismo escenario: la respuesta mejorada marcó como `SUPUESTO:` las reglas de SLA que no estaban en `ARCHITECTURE.md`. La original las había presentado como reglas fijas.
- Comparación: la versión mejorada cumple los criterios de la rúbrica de Requerimientos (negativos, RNF, supuestos). La mejora más visible fue la inclusión de RNF y preguntas abiertas, que la versión original omitía.

## Prompt revisado: ms-users/prompts/p3-arquitectura-rest.md (rama developer/fernando) (autor: Fernando · revisor: Isaac)
| Pieza | ¿Estaba? | Mejora propuesta |
|---|---|---|
| Rol | Sí | — |
| Contexto | Parcial | Agregué el proyecto, la referencia `#file:` a los requerimientos y el alcance de las HU. Pedí verificar la versión de Spring Boot en `pom.xml`. |
| Tarea | Sí | Asocié cada clase a la HU que cubre, incluidos `/me` y `/me/status`. |
| Restricciones | Parcial | Agregué que nunca se expone la entidad JPA, los códigos HTTP esperados y no agregar dependencias sin preguntar. |
| Formato de salida | Parcial | Pedí un bloque por archivo con ruta completa y una tabla final `Archivo \| Responsabilidad \| HU que cubre` (plantilla de la guía). |
| Ejemplo | Parcial | Agregué una respuesta de error RFC 9457 de ejemplo. El original solo describe los campos. |
| Verificación | Parcial | Pedí pruebas `@WebMvcTest` (201, 400, 401, 404) y unitarias con Mockito, con el comando Maven exacto (guía, Práctica 1, paso 4). |

### Antes
````markdown
# Prompt 3: Arquitectura REST por Capas

## 1. ROL
Actúa como Desarrollador Backend Senior en Java 21 y Spring Boot 3.

## 2. CONTEXTO
Microservicio ms-users. Stack: Java 21, Spring Boot 3.3.x, Spring Data JPA, Maven. Paquete base: com.ids.ms_users.

## 3. TAREA
Generar la arquitectura REST por capas completa:
1. Entidad JPA User y Enum Role.
2. DTOs para Request y Response.
3. UserRepository con Spring Data JPA.
4. TokenService y UserService con lógica de negocio y bloqueo.
5. AuthController y UserController.
6. GlobalExceptionHandler con ProblemDetail (RFC 9457).

## 4. RESTRICCIONES
- Capas estrictas: Controller -> Service -> Repository.
- DTOs obligatorios como record de Java.
- Validaciones Jakarta (@NotBlank, @Email, etc.).
- Sin valores en duro (valores en application.yaml).
- Líneas de código con longitud máxima de 120 caracteres para cumplir Checkstyle.

## 5. FORMATO DE SALIDA
Bloques de código con rutas completas relativas.

## 6. VERIFICACIÓN
Comprobar compilación limpia con ./mvnw clean compile.

## 7. SUPUESTOS / EJEMPLO
- Formato RFC 9457 incluye campos type, title, status, detail, instance e invalidParams.
````

### Después
````markdown
# Prompt 3: Arquitectura REST por Capas

## 1. ROL
Actúa como Desarrollador Backend Senior en Java 21 y Spring Boot 3.

## 2. CONTEXTO
Proyecto: Mesa de Ayuda, microservicio `ms-users` (registro, login con correo y contraseña, roles SOLICITANTE/AGENTE/ADMIN, perfil, edición y desactivación). Stack: Java 21, Spring Boot 3.3.x (verifica la versión real en `pom.xml`), Spring Data JPA, Maven. Paquete base: `com.ids.ms_users`.
Requerimientos: #file:ms-users/docs/requerimientos.md (HU de registro, login, perfil y desactivación).

## 3. TAREA
Genera la arquitectura REST por capas que cumpla las HU de gestión de usuarios:
1. Entidad JPA `User` y enum `Role`.
2. DTOs de request y response como `record`.
3. `UserRepository` con Spring Data JPA.
4. `TokenService` y `UserService` con la lógica de negocio y el bloqueo temporal por intentos fallidos de login.
5. `AuthController` (registro y login) y `UserController` (`GET /me`, `PUT /me`, `PATCH /me/status`).
6. `GlobalExceptionHandler` con `ProblemDetail` (RFC 9457).

## 4. RESTRICCIONES
- Capas estrictas: Controller -> Service -> Repository. Sin lógica de negocio en el Controller.
- DTOs como `record` de Java; nunca expongas la entidad JPA en la API.
- Validaciones con Jakarta Validation (`@NotBlank`, `@Email`, etc.).
- Errores con `@RestControllerAdvice` y formato `ProblemDetail` (RFC 9457).
- Sin valores en duro: intentos máximos, duración del bloqueo y expiración de tokens en `application.yaml`.
- Líneas de código de máximo 120 caracteres para cumplir Checkstyle.
- Códigos HTTP: 400 datos inválidos, 401 credenciales inválidas, 404 usuario no encontrado, 409 correo duplicado.
- No agregues dependencias que no estén en `pom.xml` sin preguntarme.
- Si te falta información, pregúntame antes de suponer.

## 5. FORMATO DE SALIDA
Un bloque de código por archivo, con su ruta completa como encabezado (ej. `src/main/java/com/ids/ms_users/controller/AuthController.java`). Al final, una tabla: `Archivo | Responsabilidad | HU que cubre`.

## 6. EJEMPLO
Formato RFC 9457 de un error de login (SUPUESTO: el dominio de `type` es ilustrativo):
```json
{
  "type": "https://mesa-ayuda.ids/problems/invalid-credentials",
  "title": "No autenticado",
  "status": 401,
  "detail": "Correo o contraseña incorrectos",
  "instance": "/auth/login"
}
```

## 7. VERIFICACIÓN
- Pruebas: `@WebMvcTest` del controller (201, 400, 401, 404) y unitarias de `UserService` con Mockito.
- Indica los comandos: `./mvnw clean compile` para compilar y `./mvnw -B verify` para ejecutar las pruebas.
- Ninguna respuesta incluye la entidad `User` ni el campo `password`.
````

### Resultado al probarlo
- Prueba del autor (Fernando), tarea "generar la arquitectura REST de ms-users": la respuesta con la versión mejorada usó records para DTOs, no expuso `User` y entregó la tabla final de archivos. La original no traía tabla final.
- Prueba del revisor (Isaac), misma tarea: la respuesta mejorada pidió confirmación antes de agregar una dependencia para el bloqueo, como indica la restricción. La original la agregó sin preguntar.
- Comparación: la versión mejorada produjo una estructura revisable y respetó las restricciones de capas y dependencias. La original no fijaba la salida ni las pruebas.

## Prompt revisado: ms-catalog/prompts/03-crud-capas-rest.md (rama practica-1-sebastian) (autor: Sebastián · revisor: Fernando)
| Pieza | ¿Estaba? | Mejora propuesta |
|---|---|---|
| Rol | Sí | — |
| Contexto | Sí | Cambié las referencias de archivos a la sintaxis `#file:` que recomienda la guía (Práctica 1, sección 3). |
| Tarea | Sí | — Sin cambios: el objetivo (CRUD de categorías y prioridades) es único. |
| Restricciones | Sí | Agregué "si te falta información, pregúntame antes de suponer" (Anexo A). |
| Formato de salida | Parcial | Pedí un bloque de código por archivo con ruta completa como encabezado, como indica la plantilla de la guía. |
| Ejemplo | Parcial | Agregué un ejemplo de respuesta 400 con `errors`, que la verificación exige. |
| Verificación | Parcial | Agregué pruebas (Mockito y `@WebMvcTest`) y el comando `./mvnw -B verify`. La guía pide pruebas, comando y checklist. |

### Antes
````markdown
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
````

### Después
````markdown
# Prompt 03 — CRUD REST por capas (ms-catalog)

## ROL
Actúa como desarrollador backend senior especializado en Spring Boot 4 con arquitectura MVC por capas, Spring Data JPA, Jakarta Validation y RFC 9457 (`ProblemDetail`).

## CONTEXTO
Microservicio **ms-catalog** (Java 17, Spring Boot 4.1.2, PostgreSQL 16 con Flyway). Paquete base `com.ids.ms_catalog`.
Fuentes: #file:ARCHITECTURE.md §3.2 (contrato de API), §3.4 (errores), §5.2 (estructura de paquetes), §5.3 (convenciones).
Historias: #file:docs/requerimientos.md (HU-CAT-01 a HU-CAT-07). Esquema: #file:V1__create_catalog_tables.sql.
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
- Si te falta información, pregúntame antes de suponer.

## FORMATO DE SALIDA
1. Plan breve con los archivos a crear o modificar.
2. Un bloque de código por archivo, con su ruta completa como encabezado, agrupados por paquete y en el orden de la TAREA.
3. Tabla resumen: `Archivo | Responsabilidad | HU o criterio que cubre`.

## EJEMPLO
Mapeo de error de negocio a `ProblemDetail` (409):
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
Mapeo de error de validación a `ProblemDetail` (400, con `errors`):
```json
{
  "type": "https://mesa-ayuda.ids/problems/validation-error",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "La solicitud contiene campos inválidos",
  "instance": "/api/v1/priorities/1/sla",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-11-05T07:04:35Z",
  "errors": [
    { "field": "resolutionHours", "message": "debe ser mayor o igual a responseHours" }
  ]
}
```
SUPUESTO: el formato del arreglo `errors` (`field`, `message`) es una propuesta a validar con el contrato.

## VERIFICACIÓN
- [ ] `./mvnw -B -DskipTests compile` termina sin errores.
- [ ] Pruebas unitarias del service con Mockito y `@WebMvcTest` del controller (201, 400, 404, 409) en verde con `./mvnw -B verify`.
- [ ] `POST /api/v1/categories` devuelve 201 con `Location`; el nombre repetido devuelve 409.
- [ ] `DELETE /api/v1/categories/{id}` devuelve 204 y el registro sigue existiendo con `active=false`.
- [ ] `PUT /api/v1/priorities/{id}/sla` devuelve 400 con `errors` cuando `resolutionHours < responseHours`, y 404 si la prioridad no existe.
- [ ] Ninguna respuesta expone entidades JPA ni stack traces.
````

### Resultado al probarlo
- Prueba del autor (Sebastián), tarea "implementar el CRUD de categorías y prioridades de ms-catalog": la respuesta con la versión mejorada entregó un bloque por archivo con ruta completa y el ejemplo de 400 con `errors` se reflejó en el `GlobalExceptionHandler`. La original entregó el código agrupado sin rutas en cada bloque.
- Prueba del revisor (Fernando), misma tarea: la respuesta mejorada incluyó pruebas `@WebMvcTest` y Mockito, y el comando `./mvnw -B verify`. La original solo indicó compilar sin pruebas.
- Comparación: la versión mejorada produjo una salida más fácil de revisar y de verificar. El cambio de mayor valor fue la verificación con pruebas, que la original no pedía.