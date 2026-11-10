# Instrucciones del repositorio: microservicios de la Mesa de Ayuda

Aplican a `ms-catalog/`, `ms-tickets/` y `ms-users/` (paquetes `com.ids.ms_*`). Cada regla se puede comprobar con sí/no sobre el código generado. Si una regla de un servicio concreto contradice esta, avisa antes de aplicarla. Cuando un cambio afecte a un solo servicio, trabaja solo en esa carpeta.

## Stack

1. Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway (si el servicio lo usa) y Maven (`./mvnw`). Si el código requiere otra versión o motor, es un error.
2. No agregues dependencias ni plugins al `pom.xml` sin confirmación previa del usuario.
3. Boot 4 usa Jackson 3 (`tools.jackson`) y starters modulares. Valida cada dependencia o ejemplo contra Boot 4, no contra Boot 3.

## Arquitectura

4. Capas `controller → service → repository`, sin saltos. El controller solo recibe la petición, llama a un `service` y devuelve la respuesta: sin reglas de negocio ni acceso a `repository`.
5. Las entidades JPA (`@Entity`) nunca aparecen como tipo de retorno ni de parámetro en `controller/`, y nunca salen del `service`.
6. Los DTOs de entrada y salida son `record` en `dto/request/` y `dto/response/`.
7. La conversión entre entidad y DTO se hace con métodos `static` en `mapper/`, sin MapStruct ni mapeo automático.
8. La inyección de dependencias es solo por constructor con `@RequiredArgsConstructor` y campos `private final`. Prohibido `@Autowired` en campos.
9. Lombok aparece solo en entidades y en `@RequiredArgsConstructor` de `service/` y `controller/`. Prohibido `@Data` en entidades JPA.

## Calidad de código

10. Cada método tiene 30 líneas o menos y complejidad ciclomática de 10 o menos. Si lo supera, extrae métodos privados o una clase de política.
11. Cada archivo tiene 400 líneas o menos y cada línea de código 120 caracteres o menos.
12. Sin números ni textos mágicos en Java. Límites, TTL, URLs y timeouts van en `application.yaml` con `${VAR:default}` y se leen con un record `@ConfigurationProperties`. Los textos repetidos son constantes.
13. Código, tablas, endpoints y nombres de clases en inglés. Los mensajes al usuario y el `detail` de los errores, en español.
14. No cambies endpoints, códigos HTTP ni reglas definidas en las HU (`docs/requerimientos.md`). Si una regla es ambigua, pregunta o márcala con `SUPUESTO:`.

## Errores de API

15. Los errores se manejan con `@RestControllerAdvice` y devuelven `ProblemDetail` (RFC 9457) con un campo `code` de la tabla de `ARCHITECTURE.md` (§3.4). Las excepciones de dominio extienden `DomainException`.
16. Las respuestas de error no incluyen stack trace, nombres de clases ni mensajes de excepciones internas. Un fallo no controlado responde 500 con `INTERNAL_ERROR` y se registra solo en el log del servidor. Se mantienen `server.error.include-stacktrace=never` e `include-message=never`.

## Pruebas

17. Cada regla de negocio nueva tiene al menos una prueba positiva y una negativa.
18. Service con Mockito; controller con `@WebMvcTest`; prueba de contexto con Testcontainers (`postgres:16`, `@ServiceConnection`). No uses H2 ni dependas de un PostgreSQL local.
19. Para simular beans usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`). Prohibido `@MockBean`, que no existe en Boot 4.
20. Si una prueba falla, corrige el código de producción, no lo que la prueba espera. No modifiques una prueba existente para que pase. No uses `@Disabled`.

## Base de datos

21. Todo cambio de esquema es una migración nueva `V<n>__descripcion.sql` en `src/main/resources/db/migration/`. Nunca edites una migración ya aplicada.
22. `spring.jpa.hibernate.ddl-auto` vale `validate`. Nunca `create`, `create-drop` ni `update`.
23. Todo acceso a datos usa Spring Data o `@Query` con parámetros nombrados (`:param`). Prohibido concatenar entrada del usuario en SQL o JPQL.
24. Todo DTO de entrada lleva Bean Validation (`@NotBlank`, `@Size`, `@Positive`, ...) con tamaño máximo en los textos.

## Seguridad

25. El código y los `.yaml` están libres de contraseñas, claves, tokens o IPs fijas. Los secretos se leen solo como `${VAR}` **sin valor por defecto** (por ejemplo `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, `NVD_API_KEY`). El `.env` nunca se versiona.
26. Los logs nunca contienen contraseñas, tokens, el encabezado `Authorization` ni emails. Se registran solo IDs.

## Verificación y reglas de calidad

27. Antes de dar una tarea por terminada, ejecuta `./mvnw -B verify` desde la carpeta del servicio y confirma `BUILD SUCCESS`. Reporta solo lo que ejecutaste; lo que no corriste va como "pendiente" con el motivo.
28. No uses `@SuppressWarnings`, `// NOSONAR` ni comentarios que desactiven analizadores. No desactives ni excluyas reglas de Checkstyle, PMD, SpotBugs, Dependency-Check ni JaCoCo.
29. La cobertura de líneas de JaCoCo es de 70 % o más en el total del servicio. En `ms-users`, también en las capas `service` y `controller`. No excluyas clases ni paquetes.
30. Si un análisis no se pudo ejecutar (Docker, `NVD_API_KEY` o red), no lo declares aprobado. Márcalo como pendiente y explica el bloqueo.
