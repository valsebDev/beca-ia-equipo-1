# Informe de calidad — ms-catalog (Sebastián)

Práctica 2 · Calidad como código con instrucciones y skills. Estado comparado: código original `5864b25` frente a `db813c3`.

## 1. Antes y después

| Herramienta | Regla | Antes (`5864b25`) | Después (`db813c3`) |
|---|---|---|---|
| Checkstyle 12.3.1 | MagicNumber | 11 hallazgos en 5 archivos | 0 violaciones |
| PMD 7.29.0 | Categorías bestpractices, errorprone, design, performance, multithreading | 0 | 0 |
| SpotBugs 4.10.5.0 + FindSecBugs 1.14.0 | Todas | 0 bugs | 0 bugs |
| SonarQube for IDE | S5778 (lambda de `isThrownBy` con más de una invocación) | 9 hallazgos en pruebas | 0 |
| Dependency-Check 13.0.0 | CVSS ≥ 7 | No ejecutado en el estado original | 115 dependencias (67 únicas), 0 vulnerables |
| JaCoCo 0.8.16 | Cobertura de líneas (mínimo 70 %) | 73.0 % (116/159) | 95.8 % (159/166) |
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
- **Quién lo detectó:** quality-gates-java (Dependency-Check) en una ejecución anterior.
- **Por qué era un problema:** una vulnerabilidad de XSS en la documentación de la API expuesta por el servicio.
- **Cómo lo corregí:** se fijó `org.webjars:swagger-ui` en 5.34.1 con `dependencyManagement`. Verifiqué en el paquete que swagger-ui 5.34.0 trae DOMPurify 3.4.16 y 5.34.1 trae 3.4.17. Dependency-Check da 0 vulnerables después del cambio.

## 3. Propuesta de la IA que rechacé

- **Qué proponía:** continuar sin probar la activación de la skill en un chat nuevo, y pedir cobertura o análisis de código sin especificar la skill.
- **Por qué la rechacé:** la razón de crear la skill es que, con un prompt simple para analizar código, se ejecute aunque no se haya nombrado. Sin probarlo en un chat nuevo no hay evidencia de que la descripción de la skill funcione.

## 4. Falsos positivos

No identifiqué falsos positivos en esta ejecución. Todos los hallazgos de los analizadores se corrigieron en código.

## 5. Evidencia

Últimas 15 líneas de `./mvnw -B verify` (ejecutado el 2026-11-07 con `NVD_API_KEY` tomada de `.env`):

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
[INFO] Total time:  02:06 min
[INFO] Finished at: 2026-11-07T03:47:58-06:00
[INFO] ------------------------------------------------------------------------
```

Captura de SonarQube for IDE:
- Antes: [sonarqube-servicio.png](img/sonarqube-servicio.png) (errores iniciales).
- Después: [sonarqube-servicio-limpio.png](img/sonarqube-servicio-limpio.png) (0 hallazgos de Sonar).
