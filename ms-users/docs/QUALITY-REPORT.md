# Reporte de Calidad - ms-users

Fecha: 2026-11-08
Versión de plataforma: Java 17, Spring Boot 4.1.2

## Verificación del 2026-11-13

Se ejecutó `.\mvnw.cmd -B verify` con Java 17 y Docker: **BUILD SUCCESS**,
105 pruebas, sin fallos, errores ni pruebas omitidas. Checkstyle, PMD, CPD y
SpotBugs terminaron sin hallazgos que bloqueen el build. Dependency-Check
actualizó los datos de NVD y terminó correctamente; el reporte no registró
vulnerabilidades para las dependencias analizadas. El analizador OSS Index
ya estaba desactivado en la configuración existente.

Para medir la cobertura sin acumular ejecuciones anteriores, se repitió
`verify` con un archivo JaCoCo nuevo para el reporte:

| Cobertura de líneas | Cubiertas / total | Resultado |
|---|---:|---:|
| Total | 357 / 417 | 85.61% |
| Service | 117 / 125 | 93.60% |
| Controller | 18 / 18 | 100% |

El gate configurado en el POM comprueba cobertura de **instrucciones** global,
no de líneas por capa; los porcentajes anteriores se verificaron en el reporte
JaCoCo. La configuración no se modificó.

El CI del PR #30 ejecutó las 105 pruebas correctamente, pero Dependency-Check
falló al actualizar NVD por HTTP 503. El reintento del 2026-11-13 volvió a
fallar por la misma causa; por tanto, el CI sigue pendiente, aunque la
verificación local haya terminado correctamente. No se desactivó ningún gate.

Las cifras y decisiones de las secciones siguientes son históricas. En la
revisión actual, `config/dependency-check-suppressions.xml` está vacío; no
contiene la supresión que se describe en el informe original.

### Caché independiente de NVD en CI

El [workflow de ms-users](../../.github/workflows/ci-fernando.yml) mantiene la
base de Dependency-Check en un directorio temporal del runner, separado de
la caché Maven. La clave contiene sistema operativo, servicio, versión
`13.0.0` del plugin y fecha UTC. Si no existe la entrada del día, se restaura
la más reciente con el mismo prefijo compatible. Al cambiar la versión del
plugin, se debe actualizar también la versión de la clave.

La actualización de NVD se ejecuta en un paso propio
(`dependency-check:update-only`) con hasta 3 intentos espaciados (2 y 4
minutos) ante errores como el 503. La caché se guarda con
`actions/cache/save@v4` justo después de una actualización exitosa, de modo
que se conserva aunque después fallen pruebas o analizadores. Si los tres
intentos fallan, el job termina sin guardar una caché incompleta. Después,
`verify` reutiliza los datos recién actualizados (vigencia de 240 minutos)
sin volver a consultar NVD. No se usa `autoUpdate=false` ni
`failOnError=false`; no se acepta un análisis incompleto.

GitHub limita el alcance de las cachés: un `pull_request` solo restaura
entradas de su propia referencia o de `main`, no las guardadas por push en la
rama de trabajo. Cada alcance necesita una primera descarga exitosa.

Las ejecuciones de push y pull_request de una misma rama se serializan
sin cancelar la ejecución en curso. Se conservan los triggers, las pruebas,
los umbrales y la lectura de `NVD_API_KEY` desde el secreto existente.
No se modificó el POM ni se agregaron dependencias o plugins.

| Aspecto | Antes | Con la nueva configuración |
|---|---|---|
| Datos NVD | Dependían de la restauración de la caché Maven | Caché propia, independiente de los cambios de dependencias |
| Restauración | El último CI intentó descargar 425.512 registros | Recupera una entrada compatible cuando está disponible |
| Actualizaciones y fallos | Actualización automática; 503 bloqueaba el build | Se mantiene la misma política, sin omitir el gate |

Validación local del cambio:

- `actionlint` 1.7.12 sobre el workflow: sin errores.
- `.\mvnw.cmd -B "-DdataDirectory=<copia>" dependency-check:update-only`
  con una copia de la base local: **BUILD SUCCESS**; usó el directorio
  indicado y omitió la descarga porque los datos tenían menos de 240 minutos.
- Desde `ms-users`, con Java 17 y Docker:
  `.\mvnw.cmd -B "-DdataDirectory=$env:DEPENDENCY_CHECK_DATA_DIRECTORY" verify`
  → **BUILD SUCCESS**, 105 pruebas, 0 fallos, errores u omitidas; 3:03 minutos.
- Para simular una restauración, se copió la base NVD local previamente
  actualizada a un directorio de datos independiente. Dependency-Check
  reutilizó los datos aún vigentes y ejecutó el análisis: 55 dependencias,
  0 vulnerabilidades reportadas. Checkstyle, PMD, CPD y SpotBugs:
  0 hallazgos; JaCoCo cumplido.
- **Pendiente:** comprobar guardado y restauración reales en GitHub Actions
  tras publicar estos cambios. No se ejecutó commit ni push. Una primera
  ejecución sin caché todavía requiere una descarga exitosa de NVD;
  esta validación local no demuestra que el HTTP 503 remoto haya desaparecido.

## Antes / Después

| Herramienta | Antes | Después |
|---|---|---|
| Checkstyle | 1 violación (LineLength) | 0 violaciones |
| PMD / CPD | 5 violaciones (literales y números mágicos) | 0 violaciones |
| SpotBugs | 0 violaciones | 0 violaciones |
| Dependency-Check | Bloqueado por CVE-2025-7962 (CVSS 7.5) | Aprobado tras supresión justificada |
| Cobertura JaCoCo | ~70% inicial | Service 86.39%, Controller 76.47% |

La supresión está en `config/dependency-check-suppressions.xml`. El gate `check` de JaCoCo
exige 70% de instrucciones (BUNDLE) durante `verify`.

## Métricas de Gates

| Gate | Línea base | Estado final |
|---|---|---|
| Pruebas | 26/26 correctas | 26/26 correctas |
| Checkstyle | 0 violaciones; PASS | 0 violaciones; PASS |
| PMD / CPD | 0 hallazgos; PASS | 0 hallazgos; PASS |
| SpotBugs | 0 hallazgos; PASS | 0 hallazgos; PASS |
| Dependency-Check | FAIL: CVE-2025-7962 (7.5); OSS 401 | PASS: CVE acotado; OSS Index OFF |
| JaCoCo: controllers | No medido en la línea base | 13/17 líneas; 76.47% |
| JaCoCo: service | No medido en la línea base | 127/147 líneas; 86.39% |

El scan final aún reporta CVE-2025-15104 en `hibernate-validator`; su severidad
no alcanza `failBuildOnCVSS=7.0`, por lo que Dependency-Check no bloquea el build.

## Hallazgos

### 1. Longitud de líneas y métodos

Checkstyle fija un máximo de 120 caracteres por línea. Las líneas extensas dificultan
revisar cambios y localizar condiciones; los métodos largos mezclan decisiones y
operaciones, elevando su complejidad y el costo de probarlos. PMD aplica además reglas
de buenas prácticas y errores. Se conservan las reglas sin exclusiones: estado final,
cero violaciones de Checkstyle y cero hallazgos de PMD/CPD.

### 2. Autorización en Controller o Service

La comprobación de rol dentro del controlador mezcla el protocolo HTTP con una regla
de negocio, dificulta reutilizarla y probarla sin MVC. El controlador ahora extrae los
roles del JWT y delega; `UserService` aplica la política ADMIN y lanza
`ForbiddenOperationException` si el rol no tiene permiso/autorización. Las pruebas cubren 401,
403 y 200.

### 3. CVE-2025-7962 en angus-activation

Dependency-Check asociaba CVE-2025-7962 (CVSS 7.5), un aviso de inyección SMTP de
Jakarta Mail, con `angus-activation-2.0.3` mediante una coincidencia CPE amplia. Era un
bloqueo porque el umbral del plugin es 7.0. La dependencia es transitiva de runtime
(`spring-boot-starter-data-jpa` → Hibernate → JAXB) y no se expone directamente a
datos no confiables en `ms-users`. Se acepta el riesgo de forma acotada en
`config/dependency-check-suppressions.xml`, por coordenada Maven exacta y solo para
este CVE. Debe revisarse si se cambia la versión o se mantiene.

## Decisión de Rechazo

Se rechazó fijar arbitrariamente versiones transitivas administradas por Spring Boot.
Los overrides aislados rompían la compatibilidad entre módulos de Spring Boot 4.1.2 y
sus dependencias alineadas, con riesgo de fallos de enlace o configuración en runtime.
Se mantiene la plataforma administrada y se usa una supresión estrecha para el hallazgo
aceptado, en lugar de alterar el grafo de dependencias principal.

## Evidencia

Comandos ejecutados desde `ms-users/`:

- `./mvnw -B test jacoco:report`: BUILD SUCCESS; 26 pruebas correctas.
- `./mvnw -B dependency-check:check`: BUILD SUCCESS; suppression file leído, sin consulta OSS Index.
- `./mvnw -B verify`: BUILD SUCCESS; 26 pruebas correctas y todos los gates completados.

