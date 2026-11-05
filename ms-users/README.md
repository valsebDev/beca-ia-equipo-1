# Microservicio ms-users

Microservicio de registro, autenticación y gestión de usuarios para la Mesa de Ayuda.

## Stack Tecnológico
- Java 21 (LTS)
- Spring Boot 3.3.4
- Spring Data JPA & Hibernate
- PostgreSQL 16 (en Docker)
- Flyway (Migraciones)
- Maven Wrapper

## Prerrequisitos
- Docker Desktop activo
- Java 21 configurado (`java -version`)

## Ejecución Local

1. Levantar la base de datos PostgreSQL en Docker:
```bash
docker run --name pg-mesa-ayuda -e POSTGRES_DB=mesa_ayuda -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5433:5432 -d postgres:16