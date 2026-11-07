# Prompt 12 — Configurar gates, medir y corregir en lotes (ms-catalog)

## ROL
Actúa como desarrollador Java senior que configura analizadores de calidad y corrige hallazgos en lotes pequeños y verificables.

## CONTEXTO
Microservicio **ms-catalog**. La skill `quality-gates-java` tiene los plugins en `references/maven-plugins.xml` y los rulesets en `references/checkstyle.xml` y `references/pmd-ruleset.xml`.
Restricciones del proyecto: Java 17, Maven, JaCoCo ≥ 70 %, métodos de 30 líneas o menos, complejidad 10 o menos.
Dependency-Check requiere `NVD_API_KEY`, que está en `ms-catalog/.env` (no versionado).

## TAREA
Paso 4 (configuración y tabla ANTES):
1. Propón los plugins en el `pom.xml` y muéstrame el diff. Espera aprobación.
2. Copia los rulesets a `config/checkstyle/checkstyle.xml` y `config/pmd/ruleset.xml`.
3. Ejecuta `./mvnw -B verify` con `NVD_API_KEY` si está disponible. Si no, usa `-Ddependency-check.skip=true` y marca el resultado como pendiente.
4. Entrega la tabla ANTES: Herramienta, Regla, Archivo:línea y Severidad, un resumen por regla y la cobertura de JaCoCo.

Paso 5 (corrección en lotes), en este orden:
1. Vulnerabilidades: FindSecBugs y Dependency-Check con CVSS ≥ 7.
2. Errores de SpotBugs.
3. Código muerto (PMD).
4. Complejidad.
5. Estilo (Checkstyle).
6. Cobertura: si está por debajo de 70 %, agrega pruebas positivas y negativas.

Por cada lote: máximo 5 archivos, diff y una línea de justificación por cambio. Tras cada lote, ejecuta `./mvnw -B verify` y reporta cuántos hallazgos quedan.

## RESTRICCIONES
- No uses `@SuppressWarnings`, no desactives reglas, no bajes el umbral de cobertura y no excluyas clases ni paquetes.
- No modifiques una prueba existente para que pase; corrige el código.
- Si una propuesta cae en un caso de rechazo (supresión, bajar umbral, excluir paquetes, cambiar lo que esperan las pruebas, dependencia desconocida o cambiar endpoints o códigos HTTP de las HU), no la apliques: avísame para registrarla como rechazo.
- Si un hallazgo parece falso positivo, explícalo y déjalo para discusión en el PR.
- No inventes resultados.
- No ejecutes `git commit` ni `git push`. Después de cada lote entrega el comando `P2: corrige <herramienta>/<regla> en <archivos>` y la línea para `BITACORA-IA.md`.

## FORMATO DE SALIDA
1. Diff del `pom.xml` para aprobación.
2. Tabla ANTES y resumen por regla.
3. Por lote: archivos, diff, justificación, resultado de `verify` y hallazgos restantes.
4. Rechazos propuestos y motivo.
5. Comando de commit y línea de bitácora.
