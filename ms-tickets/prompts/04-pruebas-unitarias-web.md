# Prompt 04: Pruebas unitarias y de capa web

## ROL
Eres un ingeniero de QA backend senior especializado en Spring Boot 4, JUnit 5, Mockito y Spring MVC Test.

## CONTEXTO
Microservicio `ms-tickets` (Java 17, Spring Boot 4.1.2). Capas: `controller`, `service`, `exception` (con `GlobalExceptionHandler` que devuelve `ProblemDetail` RFC 9457). Las validaciones de catálogo y usuarios se simulan con `SimulatedCatalogGateway` y `SimulatedUserGateway`. Reglas de negocio: máquina de estados en `TicketStateMachine`, SLA en `SlaCalculator` y permisos por rol en `TicketAccessPolicy`. Los cambios de la HU-01 a HU-04 ya están implementados.

## TAREA
Genera mínimo 8 pruebas automatizadas que pasen con `./mvnw test`:
1. Pruebas unitarias de `TicketService` con JUnit 5 y Mockito (`@ExtendWith`-less, con mocks de repositorios y gateways, reloj fijo con `Clock.fixed`). Cubre escenarios positivos y negativos:
   - Crear ticket válido calcula `responseDueAt` y `resolutionDueAt` y registra historial `CREATED`.
   - Crear ticket con rol distinto de `SOLICITANTE` lanza `ForbiddenException` (403).
   - Crear ticket con categoría inactiva lanza `BusinessRuleException` (422).
   - Asignar como `ADMIN` a un agente activo deja el ticket en `ASIGNADO`.
   - Asignar un ticket `CERRADO` lanza `ConflictException` (409).
   - Transición inválida (`ABIERTO` a `RESUELTO`) lanza `ConflictException` con el mensaje esperado.
   - Iniciar trabajo (`EN_PROGRESO`) por el agente asignado fija `firstResponseAt`.
   - Pasar a `CERRADO` por un agente lanza `ForbiddenException`.
   - Consultar un ticket ajeno como solicitante lanza `ForbiddenException`.
   - Listar como solicitante devuelve solo sus propios tickets.
2. Pruebas de capa web con `@WebMvcTest(TicketController.class)` y `@MockitoBean TicketService`:
   - `POST /api/v1/tickets` válido responde 201 con header `Location`.
   - `POST` con título vacío responde 400 `ProblemDetail` con `code` `VALIDATION_ERROR` y lista `errors`.
   - `GET /api/v1/tickets/{id}` inexistente responde 404 con `code` `TICKET_NOT_FOUND`.
   - `PATCH /api/v1/tickets/{id}/status` con transición inválida responde 409 con `code` `TICKET_INVALID_TRANSITION`.
   - `GET /api/v1/tickets` responde 200 con arreglo JSON.
   - Petición sin header `X-User-Role` responde 400 con `code` `MALFORMED_REQUEST`.

## RESTRICCIONES
- No uses `@SpringBootTest` ni dependas de PostgreSQL; las pruebas deben correr sin base de datos.
- Usa solo `spring-boot-starter-test` y `spring-boot-starter-webmvc-test` (ya presentes en el `pom.xml`).
- Nombres de métodos de prueba en inglés, mensajes de dominio y `detail` en español.
- No modifiques código de producción para que las pruebas pasen; reporta cualquier defecto encontrado.
- Documenta en las pruebas cualquier supuesto con el comentario `SUPUESTO:`.

## FORMATO DE SALIDA
1. Tabla: `Archivo | Responsabilidad | HU o criterio`.
2. Bloque de código con el comando de verificación y su resultado (`Tests run`, `BUILD SUCCESS`).
3. Mensaje de commit en formato `type(scope): mensaje`.

## EJEMPLO
```java
@Test
void createTicket_byAgent_throwsForbidden() {
    CreateTicketRequest request = new CreateTicketRequest("Impresora sin red", "La impresora no responde", 1L, 2L);

    assertThatThrownBy(() -> service.create(AGENT, request))
            .isInstanceOf(ForbiddenException.class);
}
```

## VERIFICACIÓN
- [ ] Hay al menos 8 pruebas y todas pasan con `.\mvnw.cmd test` dentro de `ms-tickets/`.
- [ ] Hay pruebas positivas y negativas en la capa de servicio.
- [ ] Las respuestas de error de la capa web validan `application/problem+json` y el campo `code`.
- [ ] Ninguna prueba requiere PostgreSQL activo.
- [ ] No se modificó código de producción.
