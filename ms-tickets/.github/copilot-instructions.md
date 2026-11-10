# Instrucciones del repositorio: ms-tickets

Microservicio de tickets de la Mesa de Ayuda (`com.ids.ms_tickets`). Aplica a todo el código que generes o modifiques aquí. Fuente de verdad de la arquitectura: `../ARCHITECTURE.md`. Cada regla se puede comprobar con sí/no.

## Stack

1. Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway y Maven (`./mvnw`). No agregues dependencias ni plugins sin pedir aprobación. Boot 4 usa Jackson 3 (`tools.jackson`) y starters modulares: valida cada dependencia o ejemplo contra Boot 4, no contra Boot 3.

## Arquitectura

2. Capas `controller → service → repository`, sin saltos. El controller solo recibe y devuelve DTOs y delega al service: sin `if` de negocio ni acceso a repositorios.
3. Los DTOs son `record` (paquetes `dto.request` y `dto.response`). Una entidad JPA nunca sale del service: se convierte con `TicketMapper` (manual y estático, sin MapStruct).
4. Inyección solo por constructor con `@RequiredArgsConstructor` y campos `private final`. Prohibido `@Autowired` en campos. Lombok solo en entidades y `@RequiredArgsConstructor`; nunca `@Data` en entidades JPA.

## Calidad

5. Cada método tiene ≤ 30 líneas y complejidad ciclomática ≤ 10. Si lo supera, extrae métodos privados o una clase de política (como `TicketAccessPolicy`).
6. Sin números ni textos mágicos: límites, plazos, URLs y timeouts van en `application.yaml` con `${VAR:default}` y se leen con un record `@ConfigurationProperties`. Los textos repetidos son constantes.
7. Errores solo con `GlobalExceptionHandler` (`@RestControllerAdvice`) y `ProblemDetail` (RFC 9457) con `code` según ARCHITECTURE.md §3.4. Excepciones de dominio que extienden `DomainException`. Los mensajes al usuario van en español, y el código, tablas y endpoints en inglés.
8. No cambies endpoints, códigos HTTP ni reglas definidas en las HU (`docs/requerimientos.md`). Si una regla es ambigua, pregunta o márcala con `SUPUESTO:`.

## Pruebas

9. Cada regla de negocio nueva lleva al menos una prueba positiva y una negativa. Service con Mockito, controller con `@WebMvcTest`, y la prueba de contexto con Testcontainers (`postgres:16`, `@ServiceConnection`) sin depender de un PostgreSQL local.
10. Para simular beans usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`). Nunca `@MockBean`, que no existe en Boot 4.
11. Si una prueba falla, corrige el código de producción, no lo que la prueba espera. No uses `@Disabled`, y no bajes el umbral de JaCoCo (≥ 70 % de líneas) ni excluyas paquetes.

## Base de datos

12. Todo cambio de esquema es una migración nueva `V<n>__descripcion.sql` en `src/main/resources/db/migration`. Nunca edites una migración ya aplicada. `ddl-auto` se mantiene en `validate`.

## Seguridad

13. Sin credenciales, llaves, tokens ni IPs en código, `application.yaml` ni ejemplos: usa `${VAR}` **sin valor por defecto** para secretos (`TICKETS_DB_PASSWORD`, `JWT_SECRET`). El `.env` nunca se versiona.
14. Las respuestas de error nunca incluyen stack traces, nombres de clases ni mensajes de excepciones internas: el handler devuelve un `detail` genérico y registra el detalle en el log del servidor. Se mantienen `server.error.include-stacktrace=never` e `include-message=never`.
15. Todo acceso a datos usa Spring Data o `@Query` con parámetros nombrados (`:param`). Prohibido concatenar texto en SQL o JPQL. Todo DTO de entrada lleva Bean Validation (`@NotBlank`, `@Size`, `@Positive`, ...) con tamaño máximo en los textos.
16. Los logs nunca contienen contraseñas, tokens, el encabezado `Authorization` ni emails. Se registran solo IDs.

## Terminado cuando

17. `./mvnw -B verify` termina en `BUILD SUCCESS` antes de dar por terminada cualquier tarea, y no se desactivó ninguna regla de los analizadores ni se usó `@SuppressWarnings`. Reporta solo lo que ejecutaste; lo que no corriste va como "pendiente" con el motivo.
