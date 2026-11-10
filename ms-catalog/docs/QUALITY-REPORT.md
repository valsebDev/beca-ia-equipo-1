# Informe de calidad — ms-catalog (Sebastián)

Práctica 2 · Calidad como código con instrucciones y skills. Estado comparado: código original `5864b25` frente a `db813c3`.

## 1. Antes y después

| Herramienta | Regla | Antes (`5864b25`) | Después (`db813c3`) |
|---|---|---|---|
| Checkstyle 12.3.1 | MagicNumber | 11 hallazgos en 5 archivos | 0 violaciones |
| PMD 7.29.0 | Categorías bestpractices, errorprone, design, performance, multithreading | 0 | 0 |
| SpotBugs 4.10.5.0 + FindSecBugs 1.14.0 | Todas | 0 bugs | 0 bugs |
| SonarQube for IDE | S5778 (lambda de `isThrownBy` con más de una invocación) | 9 hallazgos en pruebas | 0 |
| Dependency-Check 13.0.0 | CVSS ≥ 7 | No ejecutado en esta sesión sobre el estado original (medición con `-Ddependency-check.skip=true`) | 115 dependencias (67 únicas), 0 vulnerables (08/11/2026, 14:22) |
| JaCoCo 0.8.16 | Cobertura de líneas (mínimo 70 %) | 73.0 % (116/159) | 95.9 % (164/171) |
| Pruebas | JUnit/AssertJ | 29 | 52, 0 fallos |

Notas sobre la medición:
- El estado "antes" se midió con los goals de reporte (`checkstyle:checkstyle`, `pmd:pmd`, `spotbugs:spotbugs`, `jacoco:report`), porque `checkstyle:check` detiene el build al primer fallo.
- Los 9 hallazgos de Sonar se leyeron del panel de VS Code, con el filtro de la captura `sonarqube-servicio.png`.

## 2. Tres hallazgos

### Hallazgo 1: MagicNumber en modelo y DTOs (Checkstyle)

- **Qué era:** valores enteros escritos directamente en la lógica o en los constructores, sin nombre.
- **Por qué era un problema:** un lector que revisa la clase no sabe a simple vista qué representa ese número.
- **Cómo lo corregí:** los valores pasaron a constantes con nombre descriptivo (`static final int`) en `model/Priority.java` (`CODE_MAX_LENGTH`, `NAME_MAX_LENGTH`, `LEVEL_MAX`) y en `model/Category.java` (`NAME_MAX_LENGTH`, `DESCRIPTION_MAX_LENGTH`). Los DTOs de petición las usan en sus anotaciones `@Size`.

### Hallazgo 2: S5778 en lambdas de prueba (SonarQube)

- **Qué era:** SonarQube marca como S5778 las lambdas de `assertThrows`/`isThrownBy` que contienen más de una invocación. En estas pruebas, la lambda creaba el `PriorityRequest` y el `SlaRequest` dentro de la llamada a `priorityService.create(...)` o `updateSla(...)`, así que tenía varias operaciones.
- **Por qué era un riesgo:** si la construcción del objeto de entrada lanzaba una excepción, la prueba podía pasar por el motivo equivocado. Así, la aserción no demostraba que el servicio lanzara la excepción esperada.
- **Cómo lo corregí:** creé el request en una variable local antes del `isThrownBy`. La lambda contiene solo la llamada al servicio. Lo apliqué a 7 casos en `PriorityServiceTest` (4 de creación y 3 de `updateSla`) y a 2 en `CategoryServiceTest`. No cambié aserciones ni datos de prueba.

### Hallazgo 3: CVE en swagger-ui (Dependency-Check)

- **Qué era:** la versión de swagger-ui que trae springdoc tenía un CVE: CVE-2026-116803 y CVE-2026-116804, ambos sobre DOMPurify anterior a 3.4.17.
- **Quién lo detectó:** Dependency-Check 13.0.0, durante un `verify` con `NVD_API_KEY`, antes de fijar el override. Reportó CVE-2026-116803 y CVE-2026-116804 sobre swagger-ui 5.34.0 (DOMPurify 3.4.16).
- **Severidad:** en NVD, CVSS v3.1 6.1 (Medio) y v4.0 5.1 (Medio), con estado "Awaiting Analysis".
- **Por qué era un problema:** XSS en la documentación de la API que expone el servicio.
- **Cómo lo corregí:** fijé `org.webjars:swagger-ui` en 5.34.1 con `dependencyManagement`. Verifiqué en el paquete que 5.34.0 trae DOMPurify 3.4.16 y 5.34.1 trae 3.4.17. En la ejecución del 8 de noviembre de 2026 (14:22), Dependency-Check escaneó 115 dependencias (67 únicas), reportó 0 vulnerables y no lista swagger-ui 5.34.0.

## 3. Propuesta de la IA que rechacé

- **Qué proponía:** continuar sin probar la activación de la skill en un chat nuevo, y pedir cobertura o análisis de código sin especificar la skill.
- **Por qué la rechacé:** la razón de crear la skill es que, con un prompt simple para analizar código, se ejecute aunque no se haya nombrado. Sin probarlo en un chat nuevo no hay evidencia de que la descripción de la skill funcione.

### Rechazo técnico: Checkstyle 14.4.0

- **Qué propuso la IA:** Checkstyle 14.4.0 en `references/maven-plugins.xml` de la skill (fila 13 de la bitácora).
- **Cómo lo probé:** en un worktree temporal de `dbddd3a`, cambié la versión a 14.4.0 y ejecuté `checkstyle:check`. Falló con `UnsupportedClassVersionError`: Checkstyle 14.4.0 está compilado para class file 65 (Java 21) y el JDK del proyecto (Java 17) reconoce hasta el 61.
- **Por qué lo descarté:** el proyecto compila con Java 17 y `maven-checkstyle-plugin` 3.6.0 corre sobre ese JDK. Mantengo Checkstyle 12.3.1, con el motivo documentado en el `pom.xml`. La referencia de la skill se corrigió en `0f5df3f`.

## 4. Falsos positivos

No identifiqué falsos positivos en esta ejecución. Todos los hallazgos de los analizadores se corrigieron en código.

## 5. Evidencia

Últimas 15 líneas de `./mvnw -B verify` (ejecutado el 2026-11-08 con `NVD_API_KEY` tomada de `.env`; resultado: 52 pruebas, 0 fallos, BUILD SUCCESS):

```
[INFO]
[INFO] --- jacoco:0.8.16:report (jacoco-report) @ ms-catalog ---
[INFO] Loading execution data file /mnt/c/Users/usuar/Documents/IDS_PROCESO/Rally/practica/beca-ia-equipo-1/ms-catalog/target/jacoco.exec
[INFO] Analyzed bundle '' with 25 classes
[INFO]
[INFO] --- jacoco:0.8.16:check (jacoco-check) @ ms-catalog ---
[INFO] Loading execution data file /mnt/c/Users/usuar/Documents/IDS_PROCESO/Rally/practica/beca-ia-equipo-1/ms-catalog/target/jacoco.exec
[INFO] Analyzed bundle 'ms-catalog' with 25 classes
[INFO] All coverage checks have been met.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:18 min
[INFO] Finished at: 2026-11-08T14:22:40-06:00
[INFO] ------------------------------------------------------------------------
```

Captura de SonarQube for IDE:
- Antes: [sonarqube-servicio.png](img/sonarqube-servicio.png) (errores iniciales).
- Después: [sonarqube-servicio-limpio.png](img/sonarqube-servicio-limpio.png) (0 hallazgos de Sonar).

## 6. Alineación con la skill compartida `quality-gates-java`

Se reemplazaron `config/checkstyle/checkstyle.xml` y `config/pmd/ruleset.xml` por los de `.github/skills/quality-gates-java/references/`, que unifican las reglas de ms-catalog, ms-tickets y ms-users. El `pom.xml` no cambió.

Con esas reglas aparecieron hallazgos nuevos que se corrigieron en el código (commit `48d4396`), sin desactivar reglas:

| Herramienta | Regla | Antes | Después | Archivos |
|---|---|---|---|---|
| Checkstyle | LineLength (máx. 120) | 4 líneas | 0 | `GlobalExceptionHandler`, `PriorityService` |
| PMD | AvoidDuplicateLiterals | 4 literales en pruebas | 0 | `PriorityControllerTest`, `CategoryServiceTest`, `PriorityServiceTest` |

Resultado de `./mvnw -B verify` completo del 2026-11-09 (con Docker y `NVD_API_KEY`): BUILD SUCCESS.

| Gate | Resultado |
|---|---|
| Checkstyle | 0 violaciones |
| PMD y CPD | 0 violaciones |
| SpotBugs + FindSecBugs | 0 hallazgos (`spotbugs:check`) |
| Pruebas | 52 de 52 pasan, incluida `MsCatalogApplicationTests` con Testcontainers |
| JaCoCo | 96.05 % de líneas (170/177), mínimo 70 %; `jacoco:check` cumplido |
| Dependency-Check | Sin fallos con `failBuildOnCVSS=7` |

Avisos de PMD 7.29.0 (no bloquean el build): `UnnecessaryLocalBeforeReturn` y `AvoidLosingExceptionInformation` están marcadas para eliminación en PMD 8.0.0. Hay que reemplazarlas en la skill cuando se migre a PMD 8.

### Ajuste a las instrucciones globales (`.github/copilot-instructions.md`)

Se revisó ms-catalog contra la regla 8 de las instrucciones globales, que prohíbe `@Autowired` en campos. Las tres pruebas de controller (`CategoryControllerTest`, `CategoryErrorResponseTest`, `PriorityControllerTest`) inyectaban `MockMvc` por campo. Ahora el campo es `private final` y el constructor de la prueba lleva `@Autowired`. Las aserciones no cambiaron.

| Herramienta | Regla | Antes | Después |
|---|---|---|---|
| Pruebas | `@Autowired` por campo | 3 archivos | 0 |
| `verify` | Build | BUILD SUCCESS | BUILD SUCCESS (52 de 52 pruebas) |

