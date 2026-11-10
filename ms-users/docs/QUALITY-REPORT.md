# Reporte de Calidad - ms-users

Fecha: 2026-11-10
Versión de plataforma: Java 17, Spring Boot 4.1.2

## Antes / Después

| Herramienta | Antes | Después |
|---|---|---|
| Checkstyle | 1 violación (LineLength) | 0 violaciones |
| PMD / CPD | 5 violaciones (literales y números mágicos) | 0 violaciones |
| SpotBugs | 0 violaciones | 0 violaciones |
| Dependency-Check | Bloqueado inicialmente por CVE-2025-7962 (CVSS 7.5) | PASS: 0 CVEs reportados en CI |
| Cobertura JaCoCo | No medida en la línea base | 74% de instrucciones cubiertas en BUNDLE |

El gate `check` de JaCoCo exige 70% de instrucciones cubiertas en BUNDLE durante `verify`.
El resultado final del CI fue 74%.

## Métricas de Gates

| Gate | Línea base | Estado final |
|---|---|---|
| Pruebas | 26/26 correctas | 26/26 correctas |
| Checkstyle | 0 violaciones; PASS | 0 violaciones; PASS |
| PMD / CPD | 0 hallazgos; PASS | 0 hallazgos; PASS |
| SpotBugs | 0 hallazgos; PASS | 0 hallazgos; PASS, sin exclusiones |
| Dependency-Check | FAIL inicial por CVE-2025-7962; OSS 401 | PASS: 0 CVEs reportados; OSS Index OFF |
| JaCoCo: instrucciones BUNDLE | No medido en la línea base | 74% cubierto; PASS (umbral: 70%) |

`CVE-2025-15104` en `hibernate-validator` se observó puntualmente en una ejecución local
el 2026-11-08; no afecta el CI. El escaneo final del CI reportó 0 CVEs.

SpotBugs terminó sin exclusiones: se retiraron las anotaciones `@SuppressFBWarnings` al
sellar `TokenService` como `public final class TokenService`.

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

### 3. Dependency-Check: supresión retirada

Una ejecución anterior asoció CVE-2025-7962 con `angus-activation` mediante una coincidencia
CPE amplia. La supresión temporal se retiró porque el analizador reportó 0 coincidencias
(*zero matches*). El archivo `config/dependency-check-suppressions.xml` quedó sin supresiones
activas y el escaneo final del CI terminó con 0 CVEs reportados.

## Decisión de Rechazo

Se rechazó fijar arbitrariamente versiones transitivas administradas por Spring Boot.
Los overrides aislados podían romper la compatibilidad entre módulos de Spring Boot 4.1.2.
Se mantuvo la plataforma administrada y se retiró la supresión temporal al confirmarse
*zero matches*, sin alterar el grafo principal de dependencias.

## Evidencia

Comandos ejecutados desde `ms-users/`:

- `./mvnw -B test jacoco:report`: BUILD SUCCESS; 26 pruebas correctas.
- `./mvnw -B dependency-check:check`: BUILD SUCCESS; suppression file leído, sin consulta OSS Index.
- `./mvnw -B verify`: BUILD SUCCESS; 26 pruebas correctas y todos los gates completados.
- Dependency-Check en CI: escaneo final con 0 CVEs reportados.
- JaCoCo en CI: 74% de instrucciones cubiertas en BUNDLE (umbral: 70%).
- SonarQube for IDE: análisis limpio en `UserService`.
	Captura: [sonarqube-userservice.png](img/sonarqube-userservice.png).

