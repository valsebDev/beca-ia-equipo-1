# MS-catalog

Microservicio de **catálogo de servicio** de la Mesa de Ayuda: administra categorías, prioridades y políticas SLA. Responsable: Sebastián. Requerimientos en [docs/requerimientos.md](docs/requerimientos.md).

## Requisitos previos

- JDK 17 (`java -version`).
- PostgreSQL 16 (local o en Docker).
- Docker, solo si levantas la base de datos en contenedor.
- Maven no es necesario: el proyecto incluye `./mvnw` (Linux/macOS) y `mvnw.cmd` (Windows).

## Variables de entorno

Se leen de variables de entorno con valores por defecto en `src/main/resources/application.yaml`. Plantilla en [.env.example](.env.example).

| Variable | Por defecto | Obligatoria | Descripción |
|---|---|---|---|
| `SERVER_PORT` | `8080` | No | Puerto HTTP del servicio. |
| `CATALOG_DB_HOST` | `localhost` | No | Host de PostgreSQL. |
| `CATALOG_DB_PORT` | `5435` | No | Puerto de PostgreSQL en host. |
| `CATALOG_DB_NAME` | `catalog_db` | No | Nombre de la base. |
| `CATALOG_DB_USER` | `catalog` | No | Usuario de la base. |
| `CATALOG_DB_PASSWORD` | — | **Sí** | Contraseña de la base. No tiene valor por defecto. |
| `SWAGGER_ENABLED` | `true` | No | Activa Swagger UI. |
| `CATALOG_SLA_MAX_RESPONSE_HOURS` | `720` | No | Máximo de horas de respuesta permitido en un SLA. |
| `CATALOG_SLA_MAX_RESOLUTION_HOURS` | `2160` | No | Máximo de horas de resolución permitido en un SLA. |

## 1. Base de datos con Docker

```bash
docker run -d --name catalog-db \
  -e POSTGRES_DB=catalog_db \
  -e POSTGRES_USER=catalog \
  -e POSTGRES_PASSWORD=cambia-esta-clave \
  -p 5435:5432 \
  postgres:16
```

Flyway aplica las migraciones `V1` y `V2` automáticamente al arrancar el servicio.

## 2. Ejecutar el servicio

```bash
set -a; . ./.env; set +a      # carga las variables (Linux/macOS)
./mvnw spring-boot:run
```

En Windows (PowerShell), define las variables con `$env:CATALOG_DB_PASSWORD="..."` y ejecuta `.\mvnw.cmd spring-boot:run`.

Health check: `GET http://localhost:8080/actuator/health`.

## 3. Ejecutar las pruebas

```bash
./mvnw test
```

Incluye pruebas unitarias de servicio (JUnit 5 + Mockito) y pruebas de controlador con `@WebMvcTest`. No requieren base de datos, salvo `MsCatalogApplicationTests`, que usa Testcontainers y necesita Docker.

Para las puertas de calidad completas (Checkstyle, PMD, SpotBugs con FindSecBugs, Dependency-Check y JaCoCo con cobertura de líneas ≥ 70 %):

```bash
set -a; . ./.env; set +a      # Dependency-Check necesita NVD_API_KEY
./mvnw -B verify
```

## 4. Documentación de la API (Swagger UI)

Con el servicio en ejecución, abre <http://localhost:8080/swagger-ui.html>. La especificación OpenAPI está en <http://localhost:8080/v3/api-docs>.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/categories?active=true` | Lista categorías (filtro opcional por `active`). |
| GET | `/api/v1/categories/{id}` | Consulta una categoría. |
| POST | `/api/v1/categories` | Crea una categoría. |
| PUT | `/api/v1/categories/{id}` | Modifica nombre y descripción. |
| DELETE | `/api/v1/categories/{id}` | Desactiva la categoría (borrado lógico, 204). |
| GET | `/api/v1/priorities` | Lista prioridades con su SLA, ordenadas por nivel. |
| GET | `/api/v1/priorities/{id}` | Consulta una prioridad. |
| POST | `/api/v1/priorities` | Crea una prioridad. |
| PUT | `/api/v1/priorities/{id}/sla` | Actualiza las horas de respuesta y resolución. |

Los errores siguen RFC 9457 (`application/problem+json`) con los campos `type`, `title`, `status`, `detail`, `instance`, `code` y `timestamp`; los errores 400 de validación incluyen `errors`.

> La autenticación JWT se incorpora en la Práctica 3. En la Práctica 1 los endpoints no requieren token.
