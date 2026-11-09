# Reglas permanentes para agentes de ms-users

## Stack técnico inmutable

- Java 17 y Spring Boot 4.1.2.
- Spring Data JPA y PostgreSQL 16 para persistencia del servicio.
- H2 en memoria para pruebas automatizadas; las pruebas no requieren PostgreSQL.
- No cambies versiones ni tecnologías del stack sin autorización explícita.

## Umbrales de calidad

- Máximo 30 líneas por método y complejidad ciclomática máxima de 10.
- Máximo 400 líneas por archivo y 120 caracteres por línea de código.
- Cobertura JaCoCo mínima obligatoria de 70% en las capas service y controllers.
- Consulta los reportes de calidad y no declares éxito si una métrica no está verificada.

## Diseño y convenciones

- Los DTOs deben ser `record` de Java.
- Para entidades, Lombok se limita a `@Getter`, `@Setter` y `@NoArgsConstructor`.
- No uses `@Data` en entidades JPA.
- Usa inyección por constructor; evita la inyección por campos.
- Devuelve errores mediante `ProblemDetail`, conforme a RFC 9457.

## Seguridad y configuración

- No codifiques secretos, contraseñas ni credenciales en el código o configuración versionada.
- Inyecta el secreto JWT mediante `${JWT_SECRET}`.
- Inyecta la contraseña de datasource mediante `${SPRING_DATASOURCE_PASSWORD}`.
- Usa variables de entorno o mecanismos de secretos del entorno de ejecución.

## Regla de cierre

Antes de considerar terminado cualquier cambio, ejecuta `./mvnw -B verify` desde `ms-users/`.
Si el comando falla, informa la causa y no afirmes que la entrega está verificada.
