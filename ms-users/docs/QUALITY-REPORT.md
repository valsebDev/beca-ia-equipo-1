# Reporte de calidad — ms-users (Práctica 2)

## Tabla Antes / Después

| Herramienta / Quality Gate | Foto "Antes" (Línea Base Inicial) | Foto "Después" (Estado Final Actual) |
|----------------------------|-----------------------------------|--------------------------------------|
| **Checkstyle** | 1 infracción (*LineLength* > 120 caracteres en `UserController.java`) | **0 infracciones** (`0 Checkstyle violations`) |
| **PMD / CPD** | 5 violaciones (números mágicos, literales repetidas en `TokenService` y `UserController`) | **0 violaciones** (constantes extraídas, código limpio) |
| **SpotBugs + FindSecBugs** | 0 bugs / 0 errores de análisis estático inicial | **0 bugs / 0 errores** (`BugInstance size is 0`) |
| **OWASP Dependency-Check** | Bloqueado por `CVE-2025-7962` (CVSS 7.5 en `angus-activation`) y error 401 por falta de credenciales en OSS Index | **Aprobado** tras actualizar a la versión 13.0.0, desactivar OSS Index y añadir la supresión justificada en `config/dependency-check-suppressions.xml` |
| **JaCoCo (Cobertura)** | Sin umbral estricto en el build (solo reporte generado) | **Superado:** Service **86.39%**, Controllers **76.47%** (cumple umbral ≥ 70%) |
| **Pruebas automatizadas** | 24 pruebas iniciales con validación de seguridad pendiente | **26 pruebas, 0 fallos, 0 errores** (`BUILD SUCCESS`) |
