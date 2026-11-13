---
name: docker-springboot
description: Contenedoriza un microservicio Spring Boot 4 de este repositorio (ms-catalog, ms-tickets o ms-users; Java 17, Maven wrapper) con un Dockerfile multi-etapa, .dockerignore, servicio de base de datos y servicio en docker-compose.yml de la raíz. Úsala cuando el usuario pida Dockerfile, imagen Docker, docker compose o validar que un servicio arranca en contenedor. No la uses para cambiar lógica de negocio, endpoints ni dependencias del pom.xml.
---

# docker-springboot

Deja un servicio Spring Boot en una imagen reproducible, sin secretos ni archivos sensibles, que arranca con `docker compose` y responde `UP` en `/actuator/health`. Trabaja siempre dentro de **un único microservicio**; si no está claro cuál, pregunta. Lee antes `ARCHITECTURE.md` (§1.3, §5.1, §6.4, §7) y las reglas de `.github/copilot-instructions.md`.

## Archivos de referencia

- `references/Dockerfile.template`: plantilla multi-etapa. Reemplaza `{{SERVICE}}` y `{{VERSION}}`, y elimina las líneas `COPY` de carpetas que no existan en el servicio.

## Pasos

1. **Verifica el servicio.** Confirma en `pom.xml`: `<java.version>17</java.version>`, Spring Boot 4.x y una `<version>` que **no** termine en `-SNAPSHOT`. Si termina en `-SNAPSHOT`, detente y pregunta. Confirma en Git que `mvnw` tiene modo `100755` (`git ls-files -s <servicio>/mvnw`). Si no, pide al usuario `git update-index --chmod=+x <servicio>/mvnw`.
2. **`.dockerignore`** en la carpeta del servicio, con exactamente: `target/`, `.git/`, `.idea/`, `.vscode/`, `*.log`, `.env`. Docker lee el de la raíz del contexto, que es la carpeta del servicio.
3. **`Dockerfile`** a partir de `references/Dockerfile.template`:
   - Etapa `build` sobre `eclipse-temurin:17-jdk`: copia `mvnw`, `pom.xml` y `.mvn`, resuelve dependencias con `dependency:go-offline`, luego copia `config` y `src` y empaqueta con `-DskipTests`.
   - Etapa `runtime` sobre `eclipse-temurin:17-jre`: instala `curl` con `--no-install-recommends` y borra las listas de apt en la misma capa; crea un usuario de sistema `app`; copia solo el JAR de `target/` (sin `.original`); usa `USER app`, `EXPOSE 8080`, `HEALTHCHECK` con `curl -fsS http://localhost:8080/actuator/health`, y `ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]`.
   - Sin `ENV` ni `ARG` con credenciales. Las etiquetas de imagen siempre son versiones fijas, nunca `latest`.
4. **Compose.** Agrega en el `docker-compose.yml` de la raíz un servicio de base de datos `postgres:16` y el servicio de aplicación, en la red `mesa-ayuda`. Los secretos entran solo como `${VAR}`, sin valor por defecto. El `.env` nunca se versiona; las variables nuevas se documentan con placeholders en `.env.example`.
5. **Validación:**
   - `docker compose config -q`
   - `docker build -t <servicio>:<version> <servicio>`
   - `docker run --rm --entrypoint sh <servicio>:<version> -c "ls -A /app"` → solo `app.jar`
   - `docker run --rm --entrypoint whoami <servicio>:<version>` → `app`
   - `docker compose up -d <db> <servicio>`, esperar `healthy` y `curl -fsS http://localhost:<puerto>/actuator/health` → `{"status":"UP"}`
   - `docker compose down`
   - Si están instalados: `hadolint <servicio>/Dockerfile` y `trivy image <servicio>:<version>` (reporta HIGH/CRITICAL). Si no, márcalo como pendiente.
6. **Reportar** con una tabla `Archivo | Qué hace | Cómo se probó`, la lista de pendientes con su motivo y los comandos de commit sugeridos. No declares aprobado lo que no ejecutaste.

## Prohibiciones

- No uses `latest`, `ADD` de URLs remotas ni contraseñas en `ENV`/`ARG`.
- No copies `.env`, `.git` ni llaves dentro de la imagen.
- No cambies `pom.xml`, código Java, endpoints ni reglas de las HU sin confirmación del usuario.
- No ejecutes `git add`, `git commit`, `git push` ni `git tag`: entrega los comandos al usuario.
- No imprimas valores de `.env`.
