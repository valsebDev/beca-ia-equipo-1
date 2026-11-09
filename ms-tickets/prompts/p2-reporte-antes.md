# P2 · Reporte ANTES (ms-tickets)

Generado el 2026-11-08 con los plugins de `quality-gates-java` ya en el `pom.xml` y **sin corregir nada**. Solo se reporta lo ejecutado.

## Cómo se obtuvo

`./mvnw -B verify` se detiene en el primer plugin que falla (Checkstyle), así que para ver todos los hallazgos se corrió cada analizador por separado:

| Comando | Resultado |
|---|---|
| `./mvnw -B verify` | BUILD FAILURE en `checkstyle:check`. Antes pasaron 17 pruebas, JaCoCo `report` y JaCoCo `check`. |
| `./mvnw -B -q pmd:check` | exit 0, 0 violaciones (`target/pmd.xml` vacío, PMD 7.29.0) |
| `./mvnw -B -q spotbugs:check` | exit 0, `total_bugs='0'`, 38 clases, FindSecBugs 1.14.0 activo, umbral Medium (`target/spotbugsXml.xml`) |
| `./mvnw -B dependency-check:check` (con `NVD_API_KEY`) | BUILD FAILURE: CVSS >= 7 (`target/dependency-check-report.html`) |

## Tabla ANTES

| Herramienta | Regla | Archivo:línea | Severidad |
|---|---|---|---|
| Checkstyle | ConstantName (`log` no cumple `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`) | `src/main/java/com/ids/ms_tickets/exception/GlobalExceptionHandler.java:25` | error |
| Dependency-Check | CVE-2026-116804 (DOMPurify, bypass de SAFE_FOR_TEMPLATES) | `swagger-ui-5.34.0.jar: swagger-ui-bundle.js` y `swagger-ui-es-bundle.js` (pkg:javascript/DOMPurify@3.4.16) | CVSS 10.0 (crítica) |
| Dependency-Check | CVE-2026-116803 (DOMPurify, hook IN_PLACE) | idem | CVSS 10.0 (alta según RetireJS) |
| Dependency-Check | CVE-2026-116802 (DOMPurify, guard SMIL) | idem | sin puntuar (baja según RetireJS) |
| PMD | (ninguna) | — | — |
| SpotBugs / FindSecBugs | (ninguna) | — | — |

## Resumen por regla

| Herramienta | Regla | Archivos | Cantidad |
|---|---|---|---|
| Checkstyle | ConstantName | 1 (`GlobalExceptionHandler`) | 1 |
| Dependency-Check | CVE-2026-116804 | 2 (bundles de swagger-ui) | 1 CVE |
| Dependency-Check | CVE-2026-116803 | 2 (bundles de swagger-ui) | 1 CVE |
| Dependency-Check | CVE-2026-116802 | 2 (bundles de swagger-ui) | 1 CVE |
| PMD | — | 0 | 0 |
| SpotBugs / FindSecBugs | — | 0 | 0 |

Origen de los CVE: `springdoc-openapi-starter-webmvc-ui:3.2.0` (versión explícita en el pom, es la última publicada) arrastra `swagger-ui 5.34.0`, que empaqueta DOMPurify 3.4.16. Detectado por RetireJS.

## Cobertura JaCoCo (`target/site/jacoco/index.html`, `jacoco.csv`)

- Líneas: **216 / 264 = 81.8 %** (umbral 70 %: cumple, `All coverage checks have been met`).
- Ramas: 38 / 73 = 52.1 % (sin umbral).
- Clases con más líneas sin cubrir: `TicketService` 11, `SimulatedCatalogGateway` 9, `TicketMapper` 9, `GlobalExceptionHandler` 5.

## Avisos adicionales

- Maven avisa `Parameter 'encoding' is unknown for plugin 'maven-checkstyle-plugin:3.6.0:check'`. Es un parámetro mal puesto en el pom, no un hallazgo de código.
- Sonatype OSS Index Analyzer deshabilitado por falta de credenciales: Dependency-Check corrió sin esa fuente.
