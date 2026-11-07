# Prompt 11 — Skill quality-gates-java (ms-catalog)

## ROL
Actúa como ingeniero de calidad que escribe skills de Copilot para ejecutar y corregir puertas de calidad en proyectos Maven.

## CONTEXTO
Microservicio **ms-catalog** (Java 17, Spring Boot 4.1.2, Maven). Los gates son Checkstyle, PMD 7, SpotBugs con FindSecBugs, Dependency-Check y JaCoCo con mínimo de 70 % de líneas.
Versiones estables verificadas en Maven Central, salvo Checkstyle: 12.3.1 (la 14.4.0 exige Java 21 y el JDK del proyecto es 17). PMD 7.29.0 (maven-pmd-plugin 3.28.0), SpotBugs 4.10.5.0, FindSecBugs 1.14.0, Dependency-Check 13.0.0 y JaCoCo 0.8.16.
La skill debe vivir en `ms-catalog/.github/skills/quality-gates-java/`.

## TAREA
1. Crea `SKILL.md` con frontmatter válido: `name` y `description` con disparadores como calidad, hallazgos, cobertura, entrega y revisar calidad.
2. Secciones: Pasos con comandos, Prohibiciones y Terminado cuando. Adapta el contenido a este proyecto; no copies un ejemplo genérico.
3. Crea `references/maven-plugins.xml`, `references/checkstyle.xml` y `references/pmd-ruleset.xml`.
4. En PMD 7, quita las reglas que ya no existan y anótalo. La ruta del ruleset debe coincidir entre el pom y el archivo (`config/pmd/ruleset.xml`).
5. Prueba la skill con dos peticiones distintas sin nombrarla, por ejemplo "revisa la calidad del proyecto" y "sube la cobertura". Indica si se activó sola y qué ajustar.

## RESTRICCIONES
- No uses `@SuppressWarnings` ni desactives reglas de ningún analizador.
- No bajes el umbral de cobertura ni excluyas clases de JaCoCo.
- No inventes versiones: confírmalas en Maven Central.
- No ejecutes `git commit` ni `git push`. Entrega el comando sugerido: `git add . && git commit -m "P2: skill quality-gates-java" && git push`.

## FORMATO DE SALIDA
1. Archivos creados con su propósito.
2. Versiones verificadas y, si aplica, las reglas de PMD 7 eliminadas.
3. Resultado de las dos pruebas de activación y ajustes propuestos.
4. Texto para la bitácora y comando de commit.
