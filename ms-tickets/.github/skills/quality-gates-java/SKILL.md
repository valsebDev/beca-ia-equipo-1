---
name: quality-gates-java
description: Revisa y corrige la calidad de ms-tickets (Java 17, Spring Boot 4, Maven) con Checkstyle, PMD 7, SpotBugs/FindSecBugs, Dependency-Check y JaCoCo. Usar cuando el usuario pida revisar la calidad, ver o corregir hallazgos de un analizador, subir la cobertura, preparar la entrega o dejar el build en verde con ./mvnw -B verify. No usar para escribir funcionalidad nueva ni para cambiar contratos de API.
---

# quality-gates-java

Deja `./mvnw -B verify` en `BUILD SUCCESS` con los cinco analizadores activos y cobertura de lineas >= 70 %, sin desactivar reglas. Trabaja solo dentro de `ms-tickets/`. Las reglas del proyecto estan en `.github/copilot-instructions.md`.

## Pasos

1. **Estado inicial.** Corre `git status` y confirma que el arbol esta limpio. Si falta algun plugin, sigue el paso 2; si no, salta al 3.
2. **Configurar (solo si falta).** Los plugins estan en `references/maven-plugins.xml`.
   - Muestra el diff del `pom.xml` y **espera aprobacion** antes de editarlo.
   - Copia `references/checkstyle.xml` a `config/checkstyle/checkstyle.xml` y `references/pmd-ruleset.xml` a `config/pmd/ruleset.xml`. Las rutas deben coincidir con el pom.
   - Confirma las versiones en Maven Central antes de cambiarlas: Checkstyle debe seguir corriendo en Java 17 (hoy 12.3.1; la 13.x exige Java 21) y los 4 artefactos de PMD deben llevar la misma version.
3. **Ejecutar.** `./mvnw -B verify`. Si no hay clave NVD: `export NVD_API_KEY=...` (PowerShell: `$env:NVD_API_KEY="..."`). Sin clave: `./mvnw -B verify -Ddependency-check.skip=true` y marca Dependency-Check como **pendiente**.
4. **Reportar los hallazgos** como tabla `Herramienta | Regla | Archivo:linea | Severidad`, mas un resumen `Herramienta | Regla | Archivos | Cantidad` y la cobertura de `target/site/jacoco/index.html`. Reportes: `target/checkstyle-result.xml`, `target/pmd.xml`, `target/spotbugsXml.xml`, `target/dependency-check-report.html`.
5. **Corregir en lotes de maximo 5 archivos**, en este orden: (1) vulnerabilidades (FindSecBugs, CVSS >= 7), (2) errores de SpotBugs, (3) codigo muerto de PMD, (4) complejidad, (5) estilo de Checkstyle, (6) cobertura con pruebas positivas y negativas. Por cada cambio muestra el diff y una linea de por que.
6. **Verificar tras cada lote:** `./mvnw -B verify` e informa cuantos hallazgos quedan.

## Prohibiciones

- No desactives ni excluyas reglas, ni uses `@SuppressWarnings` ni `// NOSONAR`.
- No bajes el umbral de JaCoCo (0.70) ni excluyas clases o paquetes de la cobertura.
- No cambies endpoints, codigos HTTP ni lo que esperan las pruebas: si una prueba falla, corrige el codigo.
- No edites migraciones Flyway ya aplicadas ni agregues dependencias de produccion sin preguntar.
- No inventes resultados: si algo no se ejecuto, escribe "pendiente" y el motivo. Un falso positivo se explica y se discute, no se silencia.
- No hagas `git commit`, `git push` ni `git tag`: entrega el comando al usuario.

## Terminado cuando

- `./mvnw -B verify` termina en `BUILD SUCCESS` con Checkstyle, PMD, SpotBugs/FindSecBugs, Dependency-Check (o "pendiente" justificado) y JaCoCo.
- `target/site/jacoco/index.html` muestra cobertura de lineas >= 70 %.
- No hay reglas desactivadas, exclusiones ni `@SuppressWarnings` nuevos.
- Se entrego la tabla antes/despues y la linea para `BITACORA-IA.md`.
