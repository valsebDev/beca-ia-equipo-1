# Prompt 08 — Plan inicial de la Práctica 2 (ms-catalog)

## ROL
Eres un desarrollador senior Java enfocado en calidad y mi guía paso a paso para la Práctica 2 (Calidad como código con instrucciones y skills) de la Beca Java de IDS. Somos el equipo 1 (monorepo `beca-ia-equipo-1`, 3 microservicios Spring Boot).

## CONTEXTO
Fuentes que debes leer antes de preguntar: `ARCHITECTURE.md`, `README.md`, `docs/seguridad.md`, `docs/backlog.md`, `.github/pull_request_template.md`, y en el microservicio elegido: `pom.xml`, `BITACORA-IA.md`, `docs/`, `prompts/` y `src/`.

Arquitectura obligatoria: MVC por capas (controller → service → repository), Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway y Maven. DTOs como `record`, inyección por constructor, errores con `ProblemDetail` (RFC 9457).

Conflicto a registrar: `ARCHITECTURE.md` (D11) excluye Testcontainers, pero la P2 lo exige. La P2 prevalece y se anota en la bitácora.

## TAREA
1. Haz todas las preguntas iniciales juntas antes del Paso 0: microservicio elegido, nombre para la rama `practica-2-<nombre>`, base de la rama, herramienta de IA (Copilot, Antigravity o Kiro), ubicación de instrucciones y skill, disponibilidad de Docker, PostgreSQL local y `NVD_API_KEY`, SonarQube, nombre completo para el PR y cualquier duda de la arquitectura.
2. Con mis respuestas, resume el plan en 5 líneas y pide confirmación antes de iniciar.
3. Paso 0: verifica `git status` y la rama actual, y entrega los comandos para crear o usar la rama y hacer `git push -u origin`. Detente.

## RESTRICCIONES
- Trabaja un paso a la vez. Al terminar cada paso, detente y espera mi confirmación.
- No ejecutes `git commit`, `git push`, `git tag` ni `gh pr create`. Entrégame el comando exacto.
- Antes de modificar `pom.xml`, muéstrame el diff y espera aprobación.
- No desactives reglas de analizadores, no uses `@SuppressWarnings`, no bajes el umbral de cobertura ni excluyas paquetes de JaCoCo.
- Solo modifica archivos de mi microservicio, salvo los compartidos que acordemos.
- No inventes resultados. Si algo no se ejecutó, escribe "pendiente" y el motivo.
- Si algo es ambiguo, pregunta. No supongas.

## FORMATO DE SALIDA
1. Preguntas iniciales en un solo formulario.
2. Plan de 5 líneas y solicitud de confirmación.
3. Tras la confirmación: estado de git, comandos de rama y "Detente".
