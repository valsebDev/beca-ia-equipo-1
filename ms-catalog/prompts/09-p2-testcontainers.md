# Prompt 09 — Testcontainers para la prueba de contexto (ms-catalog)

## ROL
Actúa como desarrollador Java senior que configura pruebas de integración para Spring Boot 4.1.2 con Testcontainers 2.

## CONTEXTO
Microservicio **ms-catalog** (Java 17, Spring Boot 4.1.2, Flyway, PostgreSQL 16). La versión de Testcontainers la administra el BOM de Spring Boot 4.1.2 (`testcontainers.version` 2.0.5), así que el `pom.xml` no debe llevar `<version>`.
Hay que verificar los artefactos en Maven Central antes de escribirlos: en Testcontainers 2 `PostgreSQLContainer` está en `org.testcontainers.postgresql` y el JUnit se llama `testcontainers-junit-jupiter`.
En `ms-catalog` no existe `contextLoads`, así que hay que crearlo.

## TAREA
1. Revisa la configuración de base de datos y las pruebas existentes en `ms-catalog`.
2. Verifica en Maven Central los artefactos correctos para Boot 4.1.2 / Testcontainers 2: `spring-boot-testcontainers`, `testcontainers-postgresql` y `testcontainers-junit-jupiter`. Sin `<version>` si los administra Boot.
3. Muéstrame el diff del `pom.xml` y espera mi aprobación.
4. Crea `MsCatalogApplicationTests` con `@SpringBootTest`, `@Testcontainers`, `@Container`, `@ServiceConnection` y `postgres:16`.
5. Si hubiera `@MockBean`, migra a `@MockitoBean`.
6. Valida con `./mvnw -B verify` sin la base PostgreSQL local.

## RESTRICCIONES
- No agregues dependencias fuera de las necesarias para Testcontainers.
- No modifiques `ARCHITECTURE.md`. Registra en la bitácora la desviación de D11, que excluye Testcontainers.
- No pongas valores de conexión en duro; `@ServiceConnection` los provee.
- No ejecutes `git commit` ni `git push`. Entrega el comando sugerido: `git add . && git commit -m "P2: Testcontainers para la prueba de contexto" && git push`.

## FORMATO DE SALIDA
1. Hallazgos de la revisión y artefactos verificados.
2. Diff del `pom.xml` para aprobación.
3. Archivos creados o modificados.
4. Resultado de `./mvnw -B verify` (BUILD SUCCESS o fallo con el error).
5. Comando de commit y de push.
