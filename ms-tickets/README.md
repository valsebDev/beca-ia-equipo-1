# ms-tickets

Microservicio de tickets de la Mesa de Ayuda (Java 17, Spring Boot 4.1.2, PostgreSQL 16, Flyway).

## MS-tickets

### Requisitos previos
- JDK 17.
- Docker Desktop (o Docker Engine) para la base de datos.
- Windows PowerShell o una terminal compatible con `./mvnw`.

### Variables de entorno
| Variable | Valor por defecto | Descripción |
|---|---|---|
| `TICKETS_DB_URL` | `jdbc:postgresql://localhost:5434/tickets_db` | URL JDBC de la base de datos |
| `TICKETS_DB_USER` | `tickets` | Usuario de la base de datos |
| `TICKETS_DB_PASSWORD` | sin valor | Contraseña de la base de datos (obligatoria) |
| `SERVER_PORT` | `8080` | Puerto HTTP del servicio |
| `SERVICES_USERS_URL` | `http://localhost:8081` | URL de `ms-users` |
| `SERVICES_CATALOG_URL` | `http://localhost:8083` | URL de `ms-catalog` |
| `JWT_SECRET` | sin valor | Clave HS256 compartida con `ms-users`. Obligatoria, de al menos 32 bytes; sin ella el servicio no arranca |
| `CLIENTS_CONNECT_TIMEOUT` | `2s` | Tiempo máximo para conectar con `ms-users` y `ms-catalog` |
| `CLIENTS_READ_TIMEOUT` | `3s` | Tiempo máximo de lectura de `ms-users` y `ms-catalog` |
| `SWAGGER_ENABLED` | `true` | Habilita Swagger UI |
| `NVD_API_KEY` | sin valor | Solo para `./mvnw verify` (OWASP Dependency-Check). Se guarda en `.env`, que no se versiona |

### Seguridad
- Todas las rutas de `/api/v1/**` exigen un JWT Bearer emitido por `ms-users`. Sin token válido responden `401`; una acción no permitida al rol o al dueño responde `403`.
- El token se valida con firma HS256, `iss` y `aud`, y expiración. El algoritmo está fijado y `none` se rechaza.
- Los errores usan ProblemDetail sin stack trace (`server.error.include-stacktrace=never`).
- Las llamadas a `ms-users` y `ms-catalog` reenvían el Bearer token del usuario y devuelven `503 DEPENDENCY_UNAVAILABLE` ante fallos de dependencia.
- Hallazgos del revisor y riesgos aceptados: ver [docs/SECURITY-REPORT.md](docs/SECURITY-REPORT.md).

### Base de datos con Docker
```powershell
docker run -d --name db-tickets -e POSTGRES_DB=tickets_db -e POSTGRES_USER=tickets -e POSTGRES_PASSWORD=<contraseña> -p 5434:5432 postgres:16
```

### Ejecutar el servicio
```powershell
$env:TICKETS_DB_PASSWORD = "<contraseña>"
$env:JWT_SECRET = "<secreto-de-al-menos-32-bytes>"
./mvnw spring-boot:run
```

### Ejecutar pruebas
```powershell
./mvnw test
```

### Swagger UI
Con el servicio en ejecución, abre `http://localhost:8080/swagger-ui.html`.
