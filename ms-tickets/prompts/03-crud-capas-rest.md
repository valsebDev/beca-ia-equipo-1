# Prompt 03: CRUD por capas y API REST

## ROL
Eres un desarrollador backend senior de Spring Boot especializado en APIs REST por capas, con experiencia en RFC 9457 (ProblemDetail).

## CONTEXTO
Microservicio `ms-tickets` (Java 17, Spring Boot 4.1.2, Spring Data JPA, Lombok). Capas: `controller`, `service`, `repository`, `model`, `dto/request`, `dto/response`, `mapper`, `client` (gateways simulados), `exception` y `security` (`CurrentUser` desde headers `X-User-Id` y `X-User-Role`). Las reglas de negocio están en `TicketStateMachine`, `SlaCalculator` y `TicketAccessPolicy`. La base de datos ya existe por la migración V1.

## TAREA
Implementa el CRUD de tickets bajo `/api/v1/tickets`:
1. `POST /` crea un ticket (201 con `Location`).
2. `GET /` lista tickets según el rol, con filtro opcional `status`.
3. `GET /{id}` consulta un ticket (200, 403 o 404).
4. `PATCH /{id}/assignment` asigna un agente (solo ADMIN).
5. `PATCH /{id}/status` cambia el estado (409 si la transición es inválida, 403 si no hay permiso).
6. `GET /{id}/history` devuelve el historial append-only.

Los errores se devuelven como `ProblemDetail` con `code`, `timestamp` y, en 400, la lista `errors`.

## RESTRICCIONES
- Controladores delgados: solo validación de entrada y mapeo HTTP; la lógica va en `service`.
- DTOs como `record`; no exponer entidades JPA en la API.
- `open-in-view: false`; las transacciones se declaran en el servicio.
- Mensajes de error (`detail`) en español; nombres de clases y rutas en inglés.
- No añadir `SecurityFilterChain`: la identidad viene de headers simulados.
- No reimplementar reglas que ya existen en `TicketStateMachine` o `TicketAccessPolicy`.

## FORMATO DE SALIDA
Lista de archivos creados o modificados con su responsabilidad, seguida de una tabla `| Método | Ruta | Éxito | Errores |` con los seis endpoints.

## EJEMPLO
```java
@PostMapping
public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
        @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role) {
    TicketResponse created = ticketService.create(new CurrentUser(userId, role), request);
    return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
}
```

## VERIFICACIÓN
- [ ] `.\mvnw.cmd compile` termina con exit 0 dentro de `ms-tickets/`.
- [ ] `.\mvnw.cmd test` pasa, incluidas las pruebas de `TicketControllerTest` y `TicketServiceTest`.
- [ ] Ningún controlador contiene reglas de negocio ni accede a repositorios.
- [ ] Todas las respuestas de error usan `application/problem+json` con campo `code`.
- [ ] Swagger UI muestra los seis endpoints en `/swagger-ui.html`.
