# P2 · Prompt de corrección de hallazgos por lotes (ms-tickets)

## ROL
Eres un desarrollador senior Java enfocado en calidad. Corriges hallazgos de analizadores estáticos en `ms-tickets` con el mínimo cambio posible.

## CONTEXTO
- Microservicio `com.ids.ms_tickets`: Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway, Maven. MVC por capas (controller → service → repository). Reglas en `.github/copilot-instructions.md` y `../ARCHITECTURE.md`.
- Analizadores configurados en `pom.xml`: Checkstyle 12.3.1, PMD 7.29.0, SpotBugs 4.10.5 + FindSecBugs 1.14.0, Dependency-Check 13.0.0 y JaCoCo 0.8.16 (líneas >= 70 %).
- Hallazgos de partida: `prompts/p2-reporte-antes.md` (1 de Checkstyle, 3 CVE de DOMPurify vía swagger-ui, 0 de PMD y SpotBugs; cobertura de líneas 81.8 %).

## TAREA
Corrige los hallazgos en este orden y en lotes de máximo 5 archivos:
1. Vulnerabilidades: FindSecBugs y Dependency-Check con CVSS >= 7.
2. Errores de SpotBugs.
3. Código muerto de PMD.
4. Complejidad.
5. Estilo de Checkstyle.
6. Cobertura: si es < 70 %, agrega pruebas positivas y negativas.

## RESTRICCIONES
- No uses `@SuppressWarnings`, no desactives ni excluyas reglas, no bajes el umbral de JaCoCo ni excluyas clases o paquetes.
- No cambies endpoints, códigos HTTP ni reglas de las HU. Si una prueba falla, corrige el código, no lo que la prueba espera.
- No edites migraciones Flyway aplicadas ni agregues dependencias sin aprobación. Todo cambio en el `pom.xml` se muestra como diff y espera aprobación.
- Solo archivos dentro de `ms-tickets/`. No ejecutes `git commit`, `git push` ni `git tag`.
- Un falso positivo se explica y se discute; no se silencia.

## FORMATO
Por cada lote entrega:
1. Archivos tocados (máximo 5).
2. Por cada cambio: el diff y una línea con el porqué.
3. Resultado de `./mvnw -B verify` y cuántos hallazgos quedan por herramienta.
4. Mensaje de commit `P2: corrige <herramienta>/<regla> en <archivos>` y la línea para `BITACORA-IA.md`.

## EJEMPLO
Hallazgo: `Checkstyle | ConstantName | GlobalExceptionHandler.java:25`.
Cambio: renombrar el logger a `LOGGER` con una constante `private static final`, sin cambiar la lógica.
Porqué: Checkstyle exige nombre de constante en mayúsculas para un campo `static final`.

## VERIFICACIÓN
- `./mvnw -B verify` termina en `BUILD SUCCESS`.
- Las 17 pruebas existentes siguen pasando sin modificar sus expectativas.
- `target/site/jacoco/index.html` muestra líneas >= 70 %.
- Dependency-Check sin CVSS >= 7 (con `NVD_API_KEY` exportada).
