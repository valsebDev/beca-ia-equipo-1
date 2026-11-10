---
name: quality-gates-java
description: Ejecuta, audita y corrige las puertas de calidad de un microservicio Spring Boot de este repositorio (ms-catalog, ms-tickets o ms-users; Java 17, Maven) con Checkstyle, PMD 7 (más CPD), SpotBugs con FindSecBugs, OWASP Dependency-Check y JaCoCo. Úsala cuando el usuario pida revisar la calidad, ver o corregir hallazgos de un analizador, subir la cobertura, preparar la entrega o dejar el build en verde con ./mvnw -B verify. No la uses para escribir funcionalidad nueva ni para cambiar contratos de API.
---

# quality-gates-java

Deja `./mvnw -B verify` en `BUILD SUCCESS` con los cinco analizadores activos y cobertura de líneas ≥ 70 %, sin desactivar reglas. Trabaja siempre dentro de **un único microservicio** (`ms-catalog/`, `ms-tickets/` o `ms-users/`); si no está claro cuál, pregunta. Ejecuta todos los comandos desde su carpeta. Lee antes las reglas del proyecto (`AGENTS.md` y `.github/copilot-instructions.md`, si existen) y los archivos de `references/`. No alteres lógica de negocio fuera del alcance de los hallazgos.

## Archivos de referencia

- `references/maven-plugins.xml`: bloques `<plugin>` para el `pom.xml`, con versiones verificadas en Maven Central, todos enlazados a la fase `verify`.
- `references/checkstyle.xml`: copiar a `config/checkstyle/checkstyle.xml`.
- `references/pmd-ruleset.xml`: copiar a `config/pmd/ruleset.xml`. La ruta debe coincidir con `<rulesets>` del pom.

## Pasos

1. **Estado inicial.** `git status --short`. Si hay cambios sin relación, detente y pregunta.
2. **Configurar (solo si falta o difiere).** Compara el `pom.xml` con `references/maven-plugins.xml`.
   - Muestra el diff del `pom.xml` y **espera aprobación** antes de editarlo.
   - Copia `references/checkstyle.xml` y `references/pmd-ruleset.xml` a `config/checkstyle/checkstyle.xml` y `config/pmd/ruleset.xml`.
   - Confirma las versiones en Maven Central antes de cambiarlas: Checkstyle debe seguir corriendo en Java 17 (hoy 12.3.1; 13.x y superiores exigen Java 21) y los 4 artefactos de PMD deben llevar la misma versión.
   - **PMD 7:** si una regla del ruleset ya no existe, quítala y anótalo en el informe. No cambies la configuración de ninguna otra regla.
3. **Ejecutar.** `./mvnw -B verify`.
   - Con clave NVD: `export NVD_API_KEY=...` (PowerShell: `$env:NVD_API_KEY="..."`).
   - Sin clave: `./mvnw -B verify -Ddependency-check.skip=true` y marca Dependency-Check como **pendiente** con el motivo.
   - Para aislar un analizador: `./mvnw -B checkstyle:check`, `pmd:check`, `pmd:cpd-check`, `spotbugs:check`, `dependency-check:check`, o `./mvnw -B test jacoco:report` para cobertura.
4. **Reportar los hallazgos** como tabla `Herramienta | Regla | Archivo:línea | Severidad`, más un resumen `Herramienta | Regla | Archivos | Cantidad`. Reportes: `target/checkstyle-result.xml`, `target/pmd.xml`, `target/cpd.xml`, `target/spotbugsXml.xml`, `target/dependency-check-report.html`.
5. **Cobertura.** Lee `target/site/jacoco/index.html` (o el mensaje de `jacoco:check`); mínimo de líneas 70 %. Revisa también service y controllers por separado. No infieras cobertura por el número de pruebas.
6. **Dependency-Check.** Revisa el resumen y el reporte en `target/`. Si la base de vulnerabilidades o la red no están disponibles, documenta el bloqueo y marca el gate como **pendiente**; nunca presentes un análisis incompleto como aprobado. Una supresión solo se acepta con paquete y CVE acotados, justificación de riesgo y fecha de revisión, y nunca para ocultar un análisis incompleto. Para dependencias vulnerables, confirma compatibilidad y vuelve a correr pruebas y analizadores afectados.
7. **Corregir en lotes de máximo 5 archivos**, lee el archivo y la regla de cada hallazgo antes de editar, y sigue este orden: (1) vulnerabilidades (FindSecBugs, Dependency-Check con CVSS ≥ 7), (2) errores de SpotBugs, (3) código muerto y duplicado de PMD/CPD, (4) complejidad, (5) estilo de Checkstyle, (6) cobertura. Corrige de forma compatible con la intención de la regla. Por cada cambio muestra el diff y una línea de por qué.
8. **Cobertura < 70 %.** Agrega pruebas positivas y negativas para las reglas de negocio no cubiertas. No excluyas clases ni paquetes de JaCoCo.
9. **Verificar tras cada lote:** `./mvnw -B verify` e informa cuántos hallazgos quedan.
10. **Documentar.** Actualiza `docs/QUALITY-REPORT.md` con línea base, resultado final, hallazgos y decisiones de exclusión o rechazo, y prepara la línea para `BITACORA-IA.md`.

## Prohibiciones

- No uses `@SuppressWarnings`, `// NOSONAR` ni comentarios que desactiven analizadores.
- No desactives ni excluyas reglas de Checkstyle, PMD, SpotBugs, Dependency-Check ni JaCoCo, ni las rebajes para ocultar fallos. Un falso positivo se explica y se discute, no se silencia.
- No bajes el umbral de JaCoCo (0.70) ni excluyas clases o paquetes de la cobertura.
- No cambies endpoints ni códigos HTTP definidos en las HU, ni modifiques una prueba existente para que pase: si una prueba falla, corrige el código.
- No edites migraciones Flyway ya aplicadas ni agregues dependencias de producción sin preguntar.
- No inventes resultados: si algo no se ejecutó, escribe "pendiente" y el motivo.
- No ejecutes `git commit`, `git push`, `git tag` ni `gh pr create`: entrega el comando al usuario.
- Si una corrección propuesta cae en estos casos (dependencia inexistente o desconocida, cambio en endpoints o códigos HTTP, o cualquiera de las prohibiciones anteriores), no la apliques: avisa al usuario para registrarla como rechazo.

## Terminado cuando

- `./mvnw -B verify` termina en `BUILD SUCCESS` con Checkstyle, PMD (y CPD), SpotBugs/FindSecBugs, JaCoCo ≥ 70 % (global, service y controllers) y Dependency-Check, o con Dependency-Check marcado como pendiente y su motivo.
- No hay reglas desactivadas, exclusiones sin justificar ni `@SuppressWarnings` nuevos.
- Se entregó al usuario: archivos tocados, tabla antes/después, resultado de `verify`, hallazgos restantes, `docs/QUALITY-REPORT.md` actualizado, la línea para `BITACORA-IA.md` y el comando de commit sugerido.
