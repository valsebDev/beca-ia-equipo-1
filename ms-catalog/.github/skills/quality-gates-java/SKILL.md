---
name: quality-gates-java
description: Ejecuta y corrige las puertas de calidad de un microservicio Spring Boot de este repositorio (Checkstyle, PMD, SpotBugs con FindSecBugs, Dependency-Check y JaCoCo). Úsala cuando el usuario pida revisar calidad, revisar hallazgos, subir la cobertura, preparar una entrega, pasar los quality gates o validar que el proyecto está listo para el PR.
---

# quality-gates-java

Ejecuta `./mvnw -B verify` en el microservicio, interpreta los hallazgos de cada analizador y corrige en lotes pequeños. Trabaja siempre dentro de un único microservicio (`ms-catalog`).

## Archivos de referencia

- `references/maven-plugins.xml`: bloques `<plugin>` para `ms-catalog/pom.xml`, con versiones verificadas en Maven Central.
- `references/checkstyle.xml`: copiar a `config/checkstyle/checkstyle.xml`.
- `references/pmd-ruleset.xml`: copiar a `config/pmd/ruleset.xml`. La ruta debe coincidir con `<rulesets>` del pom.

## Pasos

Ejecuta los comandos desde `ms-catalog/`.

1. **Estado previo.** `git status --short`. Si hay cambios sin relación, detente y pregunta.
2. **Pom.** Compara `pom.xml` con `references/maven-plugins.xml`. Antes de editar el pom, muestra el diff y espera aprobación.
3. **Configuración.** Copia `references/checkstyle.xml` y `references/pmd-ruleset.xml` a `config/checkstyle/` y `config/pmd/ruleset.xml`.
4. **Ejecución.** Si existe `NVD_API_KEY`, ejecuta `./mvnw -B verify`. Si no, ejecuta `./mvnw -B verify -Ddependency-check.skip=true` y registra Dependency-Check como "pendiente" con el motivo.
5. **Hallazgos.** Lee la salida de cada herramienta y agrupa por herramienta y regla: archivo, línea y severidad.
6. **Cobertura.** Revisa `target/site/jacoco/index.html` o el mensaje de `jacoco:check`. El mínimo de líneas es 70 %.
7. **PMD 7.** Si una regla de `references/pmd-ruleset.xml` ya no existe, quítala del ruleset y anótalo en el informe. No cambies la configuración de ninguna otra regla.
8. **Lote de corrección.** Corrige como máximo 5 archivos por lote, en este orden: vulnerabilidades (FindSecBugs, Dependency-Check con CVSS ≥ 7), errores de SpotBugs, código muerto (PMD), complejidad, estilo (Checkstyle), cobertura. Tras cada lote, vuelve a ejecutar `./mvnw -B verify` y reporta cuántos hallazgos quedan.
9. **Cobertura por debajo de 70 %.** Agrega pruebas positivas y negativas para las reglas de negocio no cubiertas. No excluyas clases ni paquetes de JaCoCo.

## Prohibiciones

- No uses `@SuppressWarnings` ni comentarios que desactiven analizadores.
- No desactives reglas de Checkstyle, PMD, SpotBugs ni JaCoCo.
- No bajes el umbral de cobertura ni excluyas clases o paquetes de JaCoCo.
- No modifiques una prueba existente para que pase; corrige el código.
- No inventes resultados: solo informa lo que se ejecutó realmente.
- No ejecutes `git commit`, `git push`, `git tag` ni `gh pr create`. Entrega el comando al usuario.
- Si una corrección propuesta cae en estos casos, no la apliques: avisa al usuario para registrarla como rechazo. Son: dependencia inexistente o desconocida, cambio en endpoints o códigos HTTP definidos en las HU, o cualquiera de las prohibiciones anteriores.

## Terminado cuando

- `./mvnw -B verify` termina en BUILD SUCCESS con Checkstyle, PMD, SpotBugs con FindSecBugs, JaCoCo ≥ 70 % y Dependency-Check, o con Dependency-Check marcado como pendiente y su motivo.
- No hay reglas desactivadas ni exclusiones sin justificar.
- Se entregó al usuario el resumen: archivos tocados, resultado de `verify`, hallazgos restantes y el comando de commit sugerido.
