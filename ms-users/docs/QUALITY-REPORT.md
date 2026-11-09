# Reporte de Calidad - ms-users

Fecha: 2026-11-08
Versión de plataforma: Java 17, Spring Boot 4.1.2

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

El escaneo final aún reporta CVE-2025-15104 en `hibernate-validator`; su severidad
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
`ForbiddenOperationException` si el rol no está autorizado. Las pruebas cubren 401,
403 y 200.

### 3. CVE-2025-7962 en angus-activation

Dependency-Check asociaba CVE-2025-7962 (CVSS 7.5), un aviso de inyección SMTP de
Jakarta Mail, con `angus-activation-2.0.3` mediante una coincidencia CPE amplia. Era un
bloqueo porque el umbral del plugin es 7.0. La dependencia es transitiva de runtime
(`spring-boot-starter-data-jpa` → Hibernate → JAXB) y no se expone directamente a
datos no confiables en `ms-users`. Se acepta el riesgo de forma acotada en
`config/dependency-check-suppressions.xml`, por coordenada Maven exacta y solo para
este CVE. Debe revisarse si cambian la versión o el uso del componente.

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
