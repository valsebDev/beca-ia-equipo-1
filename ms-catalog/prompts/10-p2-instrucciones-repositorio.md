# Prompt 10 — Instrucciones del repositorio (ms-catalog)

## ROL
Actúa como arquitecto de software que escribe reglas verificables para un asistente de IA que genera código Java para este proyecto.

## CONTEXTO
Microservicio **ms-catalog**: categorías y prioridades con SLA. Paquete base `com.ids.ms_catalog`.
Fuentes: `ARCHITECTURE.md` §3.4 (errores), §5.3 (convenciones) y §6 (seguridad).
Stack: Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway, Maven.
Herramienta: GitHub Copilot. Las instrucciones van en la carpeta del servicio para evitar conflictos de merge entre compañeros.

## TAREA
1. Crea `ms-catalog/.github/copilot-instructions.md` con al menos 10 reglas verificables como sí/no, y al menos 2 de seguridad.
2. Cubre: stack y arquitectura por capas; DTOs `record`; entidades que no salen del service; inyección por constructor; métodos de 30 líneas o menos con complejidad 10 o menos; sin números ni textos mágicos; errores con `@RestControllerAdvice` y `ProblemDetail`; pruebas positiva y negativa por regla de negocio; `@MockitoBean`; migraciones `V<n>__` nuevas; `./mvnw -B verify` antes de terminar.
3. Seguridad: sin credenciales en código ni en `.yaml` (solo `${VAR}` sin valor por defecto), errores sin stack trace, SQL parametrizado, logs sin datos sensibles.
4. Prueba de activación: genera un endpoint DELETE sin mencionar las reglas y evalúa si las respetó. Entrega el texto para la bitácora.

## RESTRICCIONES
- Cada regla debe poder responderse con sí o no sobre el código generado.
- No menciones las reglas al generar el endpoint de prueba.
- No escribas el endpoint de prueba en disco; muéstralo en el chat.
- No ejecutes `git commit` ni `git push`. Entrega el comando sugerido: `git add . && git commit -m "P2: instrucciones del repositorio" && git push`.

## FORMATO DE SALIDA
1. Archivo `copilot-instructions.md` con las reglas numeradas, agrupadas por tema.
2. Código generado para la prueba de DELETE.
3. Evaluación regla por regla: cumple, no cumple o no aplica.
4. Texto para la bitácora y comando de commit.
