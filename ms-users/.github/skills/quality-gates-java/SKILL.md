---
name: quality-gates-java
description: >-
  Ejecuta y audita los quality gates del microservicio (Checkstyle, PMD,
  SpotBugs con FindSecBugs, OWASP Dependency-Check y JaCoCo), interpreta
  reportes y corrige violaciones en lotes hasta que el build esté en verde.
---

# Quality Gates de ms-users

Ejecuta este flujo desde la carpeta `ms-users/`. Revisa `AGENTS.md` y los
archivos de `references/` antes de cambiar reglas. No alteres lógica de negocio
fuera del alcance de las violaciones encontradas.

## Flujo de ejecución

1. Ejecuta los analizadores individualmente para localizar los hallazgos:

   - `./mvnw -B checkstyle:check`
   - `./mvnw -B pmd:check`
   - `./mvnw -B spotbugs:check`

   SpotBugs usa la configuración del proyecto y FindSecBugs.

2. Ejecuta pruebas y genera el reporte de JaCoCo:

   - `./mvnw -B test jacoco:report`
   - Inspecciona `target/site/jacoco/index.html` y los reportes XML/CSV.
   - Verifica cobertura de service y controllers por separado y en conjunto.
   - El umbral requerido es al menos 70%; no infieras cobertura desde el
     número de pruebas.

3. Analiza vulnerabilidades de dependencias con OWASP Dependency-Check:

   - `./mvnw -B dependency-check:check`
   - Revisa el resumen y el reporte generado en `target/`.
   - Si la base de vulnerabilidades o la red no están disponibles, documenta
     ese bloqueo; no presentes un análisis incompleto como aprobado.
   - Acepta una supresión solo con paquete y CVE acotados, justificación de riesgo
     y revisión programada; no uses supresiones para ocultar análisis incompletos.

4. Diagnostica y corrige en lotes pequeños:

   - Lee el archivo y la regla que reporta cada hallazgo antes de editar.
   - Corrige el código o configuración de manera compatible con la intención
     de la regla; no suprimas, excluyas ni rebajes Checkstyle o PMD para ocultar fallos.
   - Para dependencias vulnerables, confirma compatibilidad y ejecuta de nuevo
     pruebas y analizadores afectados.
   - Actualiza `docs/QUALITY-REPORT.md` con línea base, resultado final,
     hallazgos y decisiones de exclusión o rechazo.

## Criterio de terminado

La tarea termina solo cuando `./mvnw -B verify` finaliza en `BUILD SUCCESS`,
sin violaciones y con cobertura JaCoCo de al menos 70% en service y controllers.
Registra métricas y evidencia en `docs/QUALITY-REPORT.md`. Si algún análisis no
pudo ejecutarse, informa el bloqueo y no declares el gate aprobado.

## Referencias

- [`references/maven-plugins.xml`](references/maven-plugins.xml): fragmentos de
  configuración Maven vigentes.
- [`references/checkstyle.xml`](references/checkstyle.xml): reglas Checkstyle.
- [`references/pmd-ruleset.xml`](references/pmd-ruleset.xml): reglas PMD.
