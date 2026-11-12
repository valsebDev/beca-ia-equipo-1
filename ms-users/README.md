# Microservicio ms-users

Microservicio de registro, autenticación y gestión de usuarios para la Mesa de Ayuda.

## Stack Tecnológico
- Java 17
- Spring Boot 4.1.2
- Spring Data JPA & Hibernate
- PostgreSQL 16 (en Docker)
- Flyway (Migraciones)
- Maven Wrapper

## Prerrequisitos
- Docker Desktop activo
- Java 17 configurado (`java -version`)

## Ejecución Local

1. Levantar la base de datos PostgreSQL en Docker:
```powershell
docker run --name pg-mesa-ayuda -e POSTGRES_DB=users_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5433:5432 -d postgres:16
```

2. Desde PowerShell, configurar las variables requeridas e iniciar el servicio:
```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/users_db"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$secretBytes = New-Object byte[] 32
$rng.GetBytes($secretBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
$rng.Dispose()
./mvnw spring-boot:run
```

`JWT_SECRET` debe mantenerse fuera del repositorio. La clave generada en el ejemplo es efímera y se debe volver a configurar en cada sesión de PowerShell.

## Variables de entorno

Plantillas de referencia: [.env.example](.env.example) y `src/main/resources/templates/`. Los secretos nunca se versionan.

| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| `PORT` | No | `8081` | Puerto HTTP. |
| `SPRING_DATASOURCE_URL` | No | `jdbc:postgresql://localhost:5433/users_db` | URL de PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | Sí | — | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Sí | — | Contraseña de la base de datos. |
| `JWT_SECRET` | Sí | — | Clave HMAC de al menos 32 bytes. Si falta o es corta, el servicio no arranca. |
| `JWT_ACCESS_TTL` | No | `PT15M` | Vigencia del access token (ISO-8601). |
| `JWT_REFRESH_TTL_DAYS` | No | `7` | Días de vigencia del refresh token. |
| `BCRYPT_COST` | No | `10` | Coste de BCrypt. |
| `ADMIN_EMAIL` | No (ver nota) | — | Correo del administrador inicial. |
| `ADMIN_PASSWORD` | No (ver nota) | — | Contraseña del administrador inicial. |

Notas:
- `ADMIN_EMAIL` y `ADMIN_PASSWORD` solo se usan si todavía no existe ningún administrador. Si faltan, el servicio arranca, registra un WARN y no crea administrador. Un valor sin resolver (`${...}`) se trata como ausente.
- Actuator: solo `/actuator/health` está expuesto, sin detalles (`show-details: never`).

## Endpoints y autenticación

Contrato OpenAPI: [docs/contratos/ms-users.yaml](../docs/contratos/ms-users.yaml). Todas las rutas van bajo `/api/v1` en `http://localhost:8081`.

| Método y ruta | Acceso |
|---|---|
| `POST /api/v1/auth/register`, `/auth/login`, `/auth/refresh` | Público |
| `POST /api/v1/auth/logout` | Access token válido |
| `GET /api/v1/users/me` | Access token válido |
| `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PATCH /api/v1/users/{id}/status` | Rol ADMIN |

Los errores responden `ProblemDetail` (RFC 9457) con un campo `code`. La matriz completa de endpoint × rol y los riesgos aceptados están en [SECURITY-REPORT.md](docs/SECURITY-REPORT.md).

## Verificación

Ejecutar pruebas y verificaciones Maven con:
```powershell
./mvnw -B verify
```

Las pruebas de integración usan Testcontainers (`postgres:16`), así que Docker debe estar activo.