# Bitácora de Interacciones con IA - Práctica 1 (ms-users)

Registro estructurado de interacciones, prompts, skills y agentes empleados.

### 1. 03/11 | Gemini | prompt
- Objetivo: Generar requerimientos, actores, entidades, HU y RNF.
- Prompt o archivo: `ms-users/prompts/p1-requerimientos.md` y
	`ms-users/docs/requerimientos.md`.
- Resultado: Especificación con actores, entidades, 5 HU en Gherkin, negativos, RNF y preguntas abiertas.
- Ajuste: Ajusté el formato y agregué un ejemplo Gherkin para hacer verificables los criterios.

### 2. 03/11 | Gemini | prompt
- Objetivo: Generar modelo relacional y migración Flyway para PostgreSQL 16.
- Prompt o archivo: `ms-users/prompts/p2-database.md` y
	`ms-users/src/main/resources/db/migration/V1__crear_tablas_usuarios.sql`.
- Resultado: Migración PostgreSQL con restricciones e índices justificados.
- Ajuste: Alineé los tipos con PostgreSQL y documenté el propósito de restricciones e índices.

### 3. 04/11 | Gemini | prompt
- Objetivo: Generar arquitectura REST por capas para ms-users.
- Prompt o archivo: `ms-users/prompts/p3-arquitectura-rest.md` y
	`ms-users/src/main/java/com/ids/ms_users/controller/UserController.java`.
- Resultado: DTOs, servicios, controladores y manejo de errores ProblemDetail.
- Ajuste: Aclaré capas, validación y manejo de errores; agregué ejemplos en el prompt.

### 4. 04/11 | Gemini | agente
- Objetivo: Revisar dependencias y quality gates Maven.
- Prompt o archivo: `ms-users/pom.xml`.
- Resultado: POM con dependencias y plugins de calidad y análisis.
- Ajuste: Alineé la documentación con Java 17 y Spring Boot 4.1.2, versión declarada
	por los módulos.

### 5. 04/11 | Gemini | prompt
- Objetivo: Generar pruebas unitarias y web para servicio y controladores.
- Prompt o archivo: `ms-users/prompts/p4-pruebas.md` y pruebas de TokenService y
	UserController.
- Resultado: Pruebas de servicio, controladores y validación de tokens.
- Ajuste: Añadí pruebas para JWT alterado y autorización por rol; falta confirmar con
	`./mvnw -B verify`.

### 6. 04/11 | Gemini | agente
- Objetivo: Revisar nombres de excepciones y migración.
- Prompt o archivo: `UserNotFoundException.java` y
	`V1__crear_tablas_usuarios.sql`.
- Resultado: Se corrigió el nombre de la excepción y se revisaron las columnas.
- Ajuste: Conservé las referencias a la excepción y migración existentes y anoté el
	propósito de la corrección.

### 7. 06/11 | GitHub Copilot | agente
- Objetivo: Endurecer JWT y completar autorización de HU-05.
- Prompt o archivo: `TokenService.java`, `UserController.java`,
	`UserControllerJwtSecurityTest.java` y `docs/contratos/ms-users.yaml`.
- Resultado: JWT HS256 validado; PATCH de estado restringido a ADMIN; contrato actualizado.
- Ajuste: Eliminé el mapeo duplicado y la ruta de auto-cambio; añadí control de rol,
	errores 401/403 y pruebas con JWT para ADMIN, usuario normal, ID inexistente y tokens
	alterado o ausente. `verify` quedó pendiente.

### 8. 08/11 | GitHub Copilot | agente
- Objetivo: Alinear autorización, rutas y HU de ms-users.
- Prompt o archivo: `UserService.java`, `docs/contratos/ms-users.yaml` y `docs/backlog.md`.
- Resultado: Autorización ADMIN en `UserService`; PATCH de estado devuelve el perfil actualizado.
- Ajuste: Retiré la ruta de auto-cambio; documenté los contratos; `TokenService` devuelve
	lista vacía si faltan roles; eliminé `JwtConfiguration` y enlaces HU inconsistentes.

# Bitácora de Interacciones con IA - Práctica 2 (ms-users)

### 1. 08/11 | GitHub Copilot | agente
- Objetivo: Configurar el umbral de cobertura JaCoCo al 70%.
- Prompt o archivo: `ms-users/pom.xml`.
- Resultado: Gate `check` en `verify`, regla BUNDLE, contador INSTRUCTION y mínimo 0.70.
- Ajuste: Conservé `prepare-agent` y `report`; el build falla si la cobertura baja del 70%.

### 2. 08/11 | GitHub Copilot | agente
- Objetivo: Auditar y resolver violaciones de Checkstyle y PMD.
- Prompt o archivo: `UserController.java` y la skill `quality-gates-java`.
- Resultado: Checkstyle pasó de 1 violación a 0; PMD/CPD pasó de 5 hallazgos a 0.
- Ajuste: Reemplacé literales por constantes y corregí líneas largas sin excluir reglas.

### 3. 08/11 | GitHub Copilot | agente
- Objetivo: Evaluar CVE-2025-7962 en Dependency-Check.
- Prompt o archivo: `config/dependency-check-suppressions.xml` y `docs/QUALITY-REPORT.md`.
- Resultado: Hallazgo inicial en `angus-activation`, dependencia transitiva de runtime.
- Ajuste: La supresión temporal se retiró al reportar el analizador 0 coincidencias
	(*zero matches*); el CI final reportó 0 CVEs. No se forzaron versiones transitivas.

### 4. 08/11 | GitHub Copilot | skill
- Objetivo: Redactar reglas en AGENTS.md y crear la skill quality-gates-java.
- Prompt o archivo: `ms-users/AGENTS.md` y `.github/skills/quality-gates-java/SKILL.md`.
- Resultado: 10 reglas verificables y skill con referencias a Checkstyle, PMD y plugins Maven.
- Ajuste: Hice las reglas medibles y definí secretos como `${VAR}` sin valor por defecto;
	añadí `server.error.include-message: never`.

### 5. 10/11 | GitHub Copilot | agente
- Objetivo: Corregir el hallazgo PMD `AvoidDuplicateLiterals` tras resolver el merge con `main`.
- Situación: PMD señaló 9 apariciones del String `"code"` en `GlobalExceptionHandler`.
- Acción y corrección: Declaré `private static final String FIELD_CODE = "code";` y
	reemplacé los literales en las respuestas `ProblemDetail`.
- Decisión: No relajé la regla ni excluí el archivo.

### 6. 10/11 | GitHub Copilot | agente
- Objetivo: Resolver la violación Checkstyle `MethodLength` en `UserService.login`.
- Situación: El método superó las 30 líneas permitidas tras endurecer las reglas.
- Acción y corrección: Extraje la validación de credenciales y el bloqueo de cuenta a
	métodos privados dedicados.
- Decisión: Mantuve los quality gates estrictos, sin supresiones.