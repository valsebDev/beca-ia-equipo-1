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
| `SWAGGER_ENABLED` | `true` | Habilita Swagger UI |

### Base de datos con Docker
```powershell
docker run -d --name db-tickets -e POSTGRES_DB=tickets_db -e POSTGRES_USER=tickets -e POSTGRES_PASSWORD=<contraseña> -p 5434:5432 postgres:16
```

### Ejecutar el servicio
```powershell
$env:TICKETS_DB_PASSWORD = "<contraseña>"
./mvnw spring-boot:run
```

### Ejecutar pruebas
```powershell
./mvnw test
```

### Swagger UI
Con el servicio en ejecución, abre `http://localhost:8080/swagger-ui.html`.
