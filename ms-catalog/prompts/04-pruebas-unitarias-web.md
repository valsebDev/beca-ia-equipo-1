# Prompt 04 — Pruebas unitarias y de controlador (ms-catalog)

## ROL
Actúa como ingeniero de QA y desarrollador backend senior experto en JUnit 5, Mockito, `@WebMvcTest` y MockMvc en Spring Boot 4.

## CONTEXTO
Microservicio **ms-catalog** (`com.ids.ms_catalog`, Java 17, Spring Boot 4.1.2). Ya existen `service/` (CategoryService, PriorityService), `controller/` y `exception/GlobalExceptionHandler`.
Historias y criterios Gherkin: `docs/requerimientos.md`. Errores: `ARCHITECTURE.md` §3.4.
En Boot 4, `@WebMvcTest` está en `org.springframework.boot.webmvc.test.autoconfigure` y los mocks se declaran con `@MockitoBean`.

## TAREA
1. Pruebas unitarias de `service` con JUnit 5 y Mockito (`@ExtendWith(MockitoExtension.class)`). Cubre escenarios positivos y negativos de HU-CAT-01 a HU-CAT-07: duplicados, no encontrado, rango de SLA y orden de horas, y borrado lógico sin `delete`.
2. Pruebas de `controller` con `@WebMvcTest` y MockMvc que validen:
   - Códigos 200, 201 (con `Location`) y 204.
   - 400 por Bean Validation con `errors[].field`.
   - 400 `MALFORMED_REQUEST` por JSON ilegible o código fuera del dominio.
   - 404 y 409 con la estructura `ProblemDetail` (`code`, `status`, `detail`, `instance`).
   - 500 sin filtrar el mensaje interno.
3. Mínimo 8 pruebas en total, todas en verde con `./mvnw test`.

## RESTRICCIONES
- Nombres de prueba descriptivos en camelCase que describan el comportamiento (`createRechazaResolucionMenorQueRespuesta`).
- Usa AssertJ para las aserciones y `jsonPath` para el JSON.
- No uses base de datos, Testcontainers ni `@SpringBootTest` en las pruebas de esta práctica.
- Máximo 30 líneas por método de prueba. Sin aserciones triviales ni matchers que no verifiquen nada.
- No modifiques el código de producción salvo que una prueba revele un defecto real. Si lo haces, explica el cambio.

## FORMATO DE SALIDA
1. Lista de pruebas planeadas, agrupadas por clase y con la HU que cubren.
2. Archivos de prueba completos en `src/test/java/com/ids/ms_catalog/{service,controller}/`.
3. Resultado de `./mvnw clean test` (número de pruebas, fallos y BUILD).

## EJEMPLO
```java
@Test
void createRechazaResolucionMenorQueRespuesta() {
    assertThatThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.MEDIA, "Media", 3,
            new SlaRequest(10, 5))))
            .isInstanceOfSatisfying(DomainValidationException.class,
                    ex -> assertThat(ex.getField()).isEqualTo("resolutionHours"));
}
```

## VERIFICACIÓN
- [ ] `./mvnw clean test` termina en BUILD SUCCESS.
- [ ] Hay 8 o más pruebas y ninguna está ignorada.
- [ ] Cada HU de `requerimientos.md` tiene al menos una prueba positiva y una negativa.
- [ ] Las pruebas de 404, 409 y 400 comprueban `$.code` y la forma del `ProblemDetail`.
- [ ] No hay dependencia de PostgreSQL en la ejecución de pruebas.
