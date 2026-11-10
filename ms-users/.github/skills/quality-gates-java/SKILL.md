---
name: quality-gates-java
description: >-
  Ejecuta, audita e interpreta los quality gates de los microservicios Java (Checkstyle, PMD,
  SpotBugs con FindSecBugs, OWASP Dependency-Check y JaCoCo). Usar cuando se requiera verificar
  la calidad estática del código, resolver vulnerabilidades o preparar entregas. No la uses para 
  escribir lógica de negocio ni para cambiar versiones del stack.
---

# Quality Gates de microservicios Java

Ejecuta este flujo desde la carpeta del microservicio (`ms-users/`, `ms-tickets/`
o `ms-catalog/`). Lee `AGENTS.md` y `references/` antes de cambiar reglas.

## Pasos

1. Ejecuta cada analizador por separado para localizar hallazgos:
   `./mvnw -B checkstyle:check`, `./mvnw -B pmd:check` y `./mvnw -B spotbugs:check`.
2. Ejecuta pruebas y cobertura: `./mvnw -B test jacoco:report`. Revisa
   `target/site/jacoco/index.html`; el mínimo es 70% (0.70) y se mide, no se infiere.
3. Analiza dependencias: `./mvnw -B dependency-check:check`. Requiere la variable de
   entorno `NVD_API_KEY` (`nvdApiKeyEnvironmentVariable`); sin ella la descarga del NVD
   es lenta y puede agotar el tiempo. Si no puedes ejecutarlo, documenta el bloqueo.
4. Lee la regla y el archivo de cada hallazgo y corrige en lotes pequeños,
   volviendo a ejecutar el analizador afectado y las pruebas.
5. Documenta línea base, resultado final, hallazgos y rechazos en
   `docs/QUALITY-REPORT.md`.
6. Ejecuta `./mvnw -B verify` como validación final.

## Prohibiciones

- No suprimas, excluyas ni rebajes reglas de Checkstyle, PMD o SpotBugs.
- No bajes el umbral de JaCoCo ni `failBuildOnCVSS` (7.0).
- No suprimas un CVE sin coordenada Maven exacta, justificación y fecha de revisión.
- No declares aprobado un análisis incompleto, omitido o con la base NVD sin descargar.
- No cambies versiones del stack ni lógica de negocio fuera del alcance del hallazgo.
- No incluyas secretos ni credenciales en código o configuración versionada.

## Criterio de terminado

`./mvnw -B verify` termina en `BUILD SUCCESS`, con cero violaciones, cobertura
JaCoCo de al menos 70% y Dependency-Check ejecutado (o su bloqueo informado).

## Referencias

- [`references/maven-plugins.xml`](references/maven-plugins.xml): configuración Maven.
- [`references/checkstyle.xml`](references/checkstyle.xml): reglas Checkstyle.
- [`references/pmd-ruleset.xml`](references/pmd-ruleset.xml): reglas PMD.
