# Instrucciones para ms-catalog

Microservicio de categorías y prioridades (SLA) de la Mesa de Ayuda. Paquete base: `com.ids.ms_catalog`.
Cada regla es verificable con un sí/no sobre el código generado.

## Stack y arquitectura

1. ¿Usa Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway y Maven? Si el código requiere otra versión o motor, es un error.
2. ¿No se agrega ninguna dependencia nueva al `pom.xml` sin confirmación previa del usuario? Si se agrega sin preguntar, es un error.
3. ¿Las clases de `controller/` solo reciben la petición, llaman a un `service/` y devuelven la respuesta, sin reglas de negocio ni llamadas a `repository/`?
4. ¿Las entidades JPA (`@Entity` en `model/`) nunca aparecen como tipo de retorno ni de parámetro en `controller/`?
5. ¿Los DTOs de entrada y salida son `record` en `dto/request/` y `dto/response/`?
6. ¿La inyección de dependencias usa constructor con `@RequiredArgsConstructor`? Está prohibido `@Autowired` en campos.
7. ¿Lombok aparece solo en entidades? Está prohibido `@Data` en entidades JPA.
8. ¿La conversión entre entidad y DTO se hace con métodos `static` en `mapper/`, sin MapStruct ni mapeo automático?

## Calidad de código

9. ¿Cada método tiene 30 líneas o menos y complejidad ciclomática de 10 o menos?
10. ¿Los límites, TTL, URLs y timeouts están en `application.yaml` con `${VAR:default}`, sin números ni textos mágicos en el código Java?
11. ¿Cada regla de negocio nueva tiene al menos una prueba positiva y una negativa?
12. ¿Las pruebas usan `@MockitoBean`? Está prohibido `@MockBean`, que no existe en Spring Boot 4.
13. ¿Código, tablas, endpoints y nombres de clases están en inglés, y los mensajes al usuario y el `detail` de los errores en español?

## Errores de API

14. ¿Los errores se manejan con `@RestControllerAdvice` y devuelven `ProblemDetail` (RFC 9457) con un campo `code` de la tabla de §3.4 de ARCHITECTURE.md?

## Base de datos

15. ¿Los cambios de esquema están en una migración nueva `V<n>__descripcion.sql` en `src/main/resources/db/migration/`, sin modificar `V1` ni `V2`?
16. ¿`spring.jpa.hibernate.ddl-auto` vale `validate` (nunca `create`, `create-drop` ni `update`)?

## Seguridad

17. ¿El código y los `.yaml` están libres de contraseñas, claves, tokens o IPs fijas? Los secretos se leen solo como `${VAR}` sin valor por defecto.
18. ¿Las consultas usan parámetros (`:param` o métodos de Spring Data)? Está prohibido concatenar entrada del usuario en SQL o JPQL.
19. ¿Las respuestas de error no incluyen stack trace ni mensaje interno? Un fallo no controlado responde 500 con `INTERNAL_ERROR` y se registra solo en el log.
20. ¿Los logs evitan contraseñas, tokens, cabeceras `Authorization` y emails?

## Verificación y reglas de calidad

21. ¿Se ejecutó `./mvnw -B verify` y terminó en BUILD SUCCESS antes de dar la tarea por terminada?
22. ¿Está ausente `@SuppressWarnings`, y no se desactiva ninguna regla de Checkstyle, PMD, SpotBugs o JaCoCo?
23. ¿El umbral de cobertura de JaCoCo sigue en 70 % o más, sin excluir clases ni paquetes?
24. ¿Ninguna prueba existente se modificó para que pase? Si falla, se corrige el código.
