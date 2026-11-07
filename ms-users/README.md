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

## Verificación

Ejecutar pruebas y verificaciones Maven con:
```powershell
./mvnw -B verify
```