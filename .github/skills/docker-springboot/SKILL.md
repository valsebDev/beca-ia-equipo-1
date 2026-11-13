---
name: docker-springboot
description: Containerize one of this repository's Spring Boot microservices using the repository's Java 17, Docker, and Compose conventions.
---

# Dockerize a Spring Boot service

Use this skill only for the selected service and the shared root `docker-compose.yml`
and `.env.example`. Do not modify another service, application logic, API contracts,
or Maven dependencies.

## Repository requirements

- Follow `ARCHITECTURE.md` §§1.3, 5.1, 6.4, and 7, plus the service mapping in
  `docs/plan-contenedores.md`.
- Use Java 17 and a multi-stage build with `eclipse-temurin:17-jdk` and
  `eclipse-temurin:17-jre`. Do not use floating tags such as `latest`.
- Use the released version from the service's `pom.xml` for the image tag. If it is
  unreadable or ends in `-SNAPSHOT`, stop and ask before proceeding.
- The tracked `mvnw` must have mode `100755`. Stop and ask if it does not.
- Use the service directory as the Docker build context. Its `.dockerignore` must
  contain exactly:

  ```text
  target/
  .git/
  .idea/
  .vscode/
  *.log
  .env
  ```

- In the build stage, copy `mvnw`, `pom.xml`, and `.mvn`, download dependencies
  offline, then copy `config/` and `src/` only when those directories exist. Package
  with `./mvnw -B -q package -DskipTests`.
- The runtime stage must install `curl` with `apt-get install
  --no-install-recommends` and remove apt lists in the same layer. Run as a
  non-root system user named `app`; copy only the built JAR, excluding
  `*.jar.original`, to `/app/app.jar`.
- Expose port `8080`. The health check must call only
  `http://localhost:8080/actuator/health`. Keep the Java entry point and memory
  setting shown in `references/Dockerfile.template`.
- Do not add credentials to Dockerfile `ENV` or `ARG`, copy `.env`, `.git`, or keys
  into the image, or expose Actuator endpoints beyond `/actuator/health`.
- In Compose, add only the selected service and its database. Use `postgres:16`,
  the `mesa-ayuda` network, the documented service/DB names and host ports, and a
  `pg_isready` health check. Make the service depend only on its database with
  `condition: service_healthy`.
- Compose reads the root `.env`; a service-local `.env` is not loaded automatically.
  Keep `.env` untracked. Add only placeholders for the selected service's variables
  to `.env.example`; secret substitutions in YAML must be `${VAR}` with no default.
  If a required variable or secret is missing, stop and ask. Never print or copy
  `.env` values.

## Validation

Run the service's `./mvnw -B verify`, `docker compose config -q`, and the Docker
build and runtime checks requested for the task. Confirm the image contains only
`app.jar` under `/app`, runs as `app`, and reports healthy at the service's mapped
host port. Run `hadolint` and `trivy` if installed, report unavailable checks as
pending, and always run `docker compose down` after Compose validation.

Use `references/Dockerfile.template` as a starting point, omitting `COPY config`
or `COPY src` only when the corresponding directory is absent.
