# Reglas permanentes para agentes de ms-users

## Stack técnico inmutable

- Java 17 y Spring Boot 4.1.2.
- Spring Data JPA y PostgreSQL 16 para persistencia del servicio.
- H2 en memoria para pruebas automatizadas; las pruebas no requieren PostgreSQL.
- No cambies versiones ni tecnologías del stack sin autorización explícita.

## Reglas verificables

Cada regla se valida con Sí/No. Una respuesta No bloquea la entrega.

### Arquitectura y diseño

1. **MVC estricto:** ¿El Controller solo maneja HTTP y delega la lógica de negocio al Service,
   y el acceso a datos queda solo en el Repository?
2. **Métodos cortos:** ¿Todo método tiene 30 líneas o menos?
3. **Complejidad ciclomática:** ¿Todo método tiene complejidad ciclomática de 10 o menos?
4. **DTOs inmutables:** ¿Todos los DTOs son `record` de Java?

### Pruebas y cobertura

5. **MockMvc standalone:** ¿Los tests de controllers usan `MockMvcBuilders.standaloneSetup(...)`
   en lugar de levantar el contexto completo de Spring?
6. **Cobertura mínima:** ¿JaCoCo reporta cobertura de instrucciones de 70% o más
   (gate `check` en `verify`)?

### Seguridad

7. **Cero secretos en duro:** ¿Las credenciales y secretos se leen con `${VAR}`
   (p. ej. `${JWT_SECRET}`, `${SPRING_DATASOURCE_PASSWORD}`) sin valores reales versionados?
8. **Sin fuga de errores:** ¿`application.yaml` define `server.error.include-message: never`
   para no exponer mensajes ni stack traces?

### Estilo y quality gates

9. **Longitud de línea:** ¿Ninguna línea supera 120 caracteres (regla de Checkstyle)?
10. **Verificación final:** ¿`./mvnw -B verify` desde `ms-users/` termina en `BUILD SUCCESS`
    antes de entregar? Si falla, informa la causa y no declares la entrega verificada.
