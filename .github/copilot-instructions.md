# Instrucciones del repositorio (monorepo beca-ia-equipo-1)

Aplican a `ms-users`, `ms-tickets` y `ms-catalog`. Fuente de verdad de la arquitectura:
`ARCHITECTURE.md` de cada servicio. Cada regla se puede comprobar con sí/no.
`AGENTS.md` en la raíz es copia idéntica de este archivo.

## Stack

1. Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway y Maven (`./mvnw`). No agregues dependencias
   ni plugins sin aprobación. Boot 4 usa Jackson 3 (`tools.jackson`) y starters modulares: valida
   cada dependencia o ejemplo contra Boot 4, no contra Boot 3.

## Arquitectura

2. Capas `controller → service → repository`, sin saltos. El controller solo recibe y devuelve
   DTOs y delega al service: sin `if` de negocio ni acceso a repositorios.
3. Los DTOs son `record` (paquetes `dto.request` y `dto.response`). Una entidad JPA nunca sale
   del service: se convierte con un mapper manual y estático, sin MapStruct.
4. Inyección solo por constructor con `@RequiredArgsConstructor` y campos `private final`.
   Prohibido `@Autowired` en campos. Lombok solo en entidades y `@RequiredArgsConstructor`;
   nunca `@Data` en entidades JPA.

## Calidad

5. Cada método tiene 30 líneas o menos y complejidad ciclomática de 10 o menos. Si lo supera,
   extrae métodos privados o una clase de política.
6. Cada línea de código tiene 120 caracteres o menos (Checkstyle).
7. Sin números ni textos mágicos: límites, plazos, URLs y timeouts van en `application.yaml` con
   `${VAR:default}` y se leen con un record `@ConfigurationProperties`. Los textos repetidos
   son constantes.
8. Errores solo con `GlobalExceptionHandler` (`@RestControllerAdvice`) y `ProblemDetail`
   (RFC 9457). Excepciones de dominio que extienden `DomainException`. Mensajes al usuario en
   español; código, tablas y endpoints en inglés.
9. No cambies endpoints, códigos HTTP ni reglas definidas en las HU (`docs/requerimientos.md`).
   Si una regla es ambigua, pregunta o márcala con `SUPUESTO:`.

## Pruebas

10. Cada regla de negocio nueva lleva al menos una prueba positiva y una negativa. Service con
    Mockito y controller con `@WebMvcTest` o MockMvc standalone, sin dependencias externas.
11. La prueba de contexto es aislada: H2 en memoria en ms-users y Testcontainers
    (`postgres:16`, `@ServiceConnection`) en ms-tickets, sin PostgreSQL local.
12. Para simular beans usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
    Nunca `@MockBean`, que no existe en Boot 4.
13. Si una prueba falla, corrige el código de producción, no lo que la prueba espera. No uses
    `@Disabled`. JaCoCo exige 70 % o más (0.70) en la fase `verify`: no lo bajes ni excluyas paquetes.

## Base de datos

14. Todo cambio de esquema es una migración nueva `V<n>__descripcion.sql` en
    `src/main/resources/db/migration`. Nunca edites una migración ya aplicada.
    `ddl-auto` se mantiene en `validate`.

## Seguridad

15. Sin credenciales, llaves, tokens ni IPs en código, `application.yaml` ni ejemplos: usa
    `${VAR}` sin valor por defecto para secretos (`JWT_SECRET`, contraseñas de base de datos).
    El `.env` nunca se versiona.
16. Las respuestas de error nunca incluyen stack traces, nombres de clases ni mensajes internos:
    el handler devuelve un `detail` genérico y registra el detalle en el log. Se mantienen
    `server.error.include-stacktrace=never` y `server.error.include-message=never`.
17. Todo acceso a datos usa Spring Data o `@Query` con parámetros nombrados (`:param`). Prohibido
    concatenar texto en SQL o JPQL. Todo DTO de entrada lleva Bean Validation (`@NotBlank`,
    `@Size`, `@Positive`, ...) con tamaño máximo en los textos.
18. Los logs nunca contienen contraseñas, tokens, el encabezado `Authorization` ni emails.
    Se registran solo IDs.

## Skill compartida

Para linters, cobertura, vulnerabilidades o `verify`, usa `.github/skills/quality-gates-java/`.

## Terminado cuando

19. `./mvnw -B verify` termina en `BUILD SUCCESS` antes de dar por terminada cualquier tarea, sin
    `@SuppressWarnings` ni reglas de analizadores desactivadas. Reporta solo lo que ejecutaste;
    lo que no corriste va como "pendiente" con el motivo.
