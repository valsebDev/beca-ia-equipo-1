# Prompt 1: Generación de requerimientos y HU.
## ROL

Actúa como analista de requerimientos senior con experiencia en arquitectura de microservicios.



## CONTEXTO

Microservicio `ms-users` de un sistema de Mesa de Ayuda en Java 21 y Spring Boot 3. 

Texto de negocio: "El microservicio ms-users maneja registro, autenticación (login con correo y contraseña), emisión de tokens JWT, roles (SOLICITANTE, AGENTE, ADMIN), consulta y edición de perfil, desactivación de cuentas y bloqueo temporal por intentos fallidos de login."



## TAREA

Identifica los actores, entidades principales y reglas de negocio.
2. Genera 5 historias de usuario (HU) con el formato: "Como <rol> quiero <acción> para <beneficio>".

3. Para cada HU redacta al menos 2 escenarios en notación Gherkin (Dado/Cuando/Entonces), asegurando que al menos 1 escenario sea negativo.

4. Define 3 requerimientos no funcionales (RNF) sobre seguridad, auditoría y formato de errores.

5. Lista las preguntas abiertas que harías al cliente.



## RESTRICCIONES

No inventes reglas que no correspondan a un módulo de usuarios. Si asumes algo, márcalo explícitamente como SUPUESTO.
Los escenarios negativos deben especificar códigos HTTP claros (ej. 400, 401, 409).


## FORMATO DE SALIDA

Documento Markdown estructurado con secciones: Actores, Entidades, Reglas de Negocio, Historias de Usuario (con Gherkin), RNF y Preguntas Abiertas.



## VERIFICACIÓN

Verifica que cada HU tenga al menos un escenario negativo en Gherkin.

# Prompt 2: Modelo relacional y migración Flyway.
## ROL
Actúa como Administrador de Bases de Datos especializado en PostgreSQL 16.

## CONTEXTO
Microservicio `ms-users` basado en las historias de usuario de #file:ms-users/docs/requerimientos.md. Motor de base de datos: PostgreSQL 16. Herramienta de migración: Flyway.

## TAREA
1. Propón el modelo relacional para la tabla `users` (id, user_identifier, name, paternal_name, maternal_name, email, password, role, is_active, created_at, jwt_id, jwt_expires_at, refresh_token_hash, refresh_token_expires_at).
2. Genera el script de migración SQL para Flyway compatible con PostgreSQL 16.
3. Justifica cada índice propuesto mediante comentarios en el SQL con base en las búsquedas frecuentes de las HU.

## RESTRICCIONES
- Usa tipos de datos nativos de PostgreSQL (BIGSERIAL, VARCHAR, TIMESTAMP, BOOLEAN).
- Incluye restricción UNIQUE en el correo electrónico.
- No incluyas sintaxis específica de H2 ni MySQL.

## FORMATO DE SALIDA
Genera un único bloque de código SQL listo para guardarse en `V1__crear_tablas_usuarios.sql`.

## VERIFICACIÓN
Verifica que las restricciones NOT NULL, UNIQUE y los índices tengan su correspondiente justificación en comentarios.

# Prompt 3: Arquitectura REST (DTOs, Service, Controller, ExceptionHandler).
## ROL
Actúa como Desarrollador Backend Senior en Java 21 y Spring Boot 3.

## CONTEXTO
Microservicio `ms-users` del proyecto Mesa de Ayuda. Stack: Java 21, Spring Boot 3.x, Spring Data JPA, Maven. Paquete base: `com.ids.ms_users`. Requerimientos en #file:ms-users/docs/requerimientos.md.

## TAREA
Genera la arquitectura REST completa para `ms-users` que cubra las HU de gestión de usuarios (Registro, Login, Perfil `GET /me`, Edición `PUT /me` y Desactivación `PATCH /me/status`).

## RESTRICCIONES
- Arquitectura estricta por capas: Controller -> Service -> Repository. Sin lógica en el Controller.
- DTOs obligatorios como `record` de Java (nunca expongas entidades JPA).
- Validaciones de entrada usando Jakarta Validation (`@NotBlank`, `@Email`, etc.).
- Manejo global de excepciones mediante `@RestControllerAdvice` con formato `ProblemDetail` (RFC 9457).
- Configuración externa en `application.yaml`, nada de valores en duro (hardcoded).

## FORMATO DE SALIDA
Genera un bloque de código por archivo Java con su ruta completa como encabezado (ej. `src/main/java/com/ids/ms_users/controller/UsersController.java`).

## VERIFICACIÓN
Indica los comandos de compilación Maven para verificar que no existan errores de sintaxis o dependencias faltantes.

# Prompt 4: Automatización de pruebas unitarias y de controlador.
## ROL
Actúa como Ingeniero de Pruebas (QA Automation) en Java/Spring Boot.

## CONTEXTO
Microservicio `ms-users`. Necesitamos probar el comportamiento del controlador y la capa de servicio basándonos en los escenarios Gherkin definidos en #file:ms-users/docs/requerimientos.md.

## TAREA
1. Genera las pruebas unitarias para `UsersService` usando JUnit 5 y Mockito.
2. Genera las pruebas de controlador para `UsersController` usando `@WebMvcTest` y `MockMvc`.
3. Asegura la cobertura tanto de escenarios positivos (200, 201) como negativos (400, 401, 404, 409).

## RESTRICCIONES
- Mínimo 8 métodos de prueba independientes en total.
- Los nombres de las pruebas deben ser descriptivos (ej. `login_credencialesInvalidas_devuelve401`).
- Usa la sintaxis moderna de JUnit 5 (`Assertions.*`) y Mockito (`given/when/then`).

## FORMATO DE SALIDA
Archivos de prueba Java estructurados con rutas completas.

## VERIFICACIÓN
Indica el comando Maven exacto para ejecutar las pruebas (`./mvnw -B verify`) y verificar que pasen en verde.
