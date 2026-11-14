---
name: docker-springboot
description: Contenedoriza un microservicio Spring Boot 4 de este repositorio (ms-catalog, ms-tickets o ms-users; Java 17, Maven) con Dockerfile multi-stage, .dockerignore, HEALTHCHECK y servicio en docker-compose.yml. Úsala cuando el usuario pida crear o revisar el Dockerfile, la imagen, el .dockerignore o la integración en compose de un servicio. No la uses para cambiar lógica de negocio, endpoints ni dependencias del pom.xml.
---

# docker-springboot

Genera una imagen **multi-stage**, reproducible y mínima para **un único microservicio** (`ms-catalog/`, `ms-tickets/` o `ms-users/`). Trabaja siempre dentro de la carpeta de ese servicio; si no está claro cuál, pregunta. Lee antes `ARCHITECTURE.md` (§1.3 despliegue, §6.4 secretos) y `.github/copilot-instructions.md`.

Plantilla base: `references/Dockerfile.template`. Sustituye los marcadores `{{...}}` por los valores del servicio; no copies la plantilla sin revisar.

## Reglas del proyecto que aplican

- Java 17 (`java.version` del `pom.xml`) y Spring Boot 4.1.2. No cambies la versión de Java ni de la imagen base sin confirmación.
- Actuator expone **solo** `/actuator/health`. El HEALTHCHECK usa esa ruta y nada más.
- Sin `latest`. La imagen base lleva la versión fija que indique el usuario o el prompt de la tarea.
- Sin credenciales en `ENV` ni `ARG`. Los secretos llegan en tiempo de ejecución desde `docker-compose.yml` y `.env`.
- Sin `.env`, `.git`, claves, `target/` con fuentes ni IDE dentro de la imagen. Lo controla `.dockerignore`.
- Usuario no root (`USER app`).
- No añadas dependencias ni plugins al `pom.xml` para construir la imagen. El build usa `./mvnw`.
- No modifiques `src/`, `config/` ni endpoints.

## Pasos

1. **Estado inicial.** `git status --short` y confirma que `{{SERVICIO}}/mvnw` tiene modo `100755` en git (`git ls-files -s {{SERVICIO}}/mvnw`). Si no, detente y pide corregirlo.
2. **Verifica la versión del pom.** Debe ser una versión fija, sin `-SNAPSHOT` salvo que el usuario lo confirme. Esa versión se usa en la etiqueta de la imagen.
3. **Confirma la estructura.** Deben existir `pom.xml`, `mvnw`, `.mvn/` y `src/`. Incluye `config/` solo si existe. Si falta una pieza, quita su línea `COPY` de la plantilla.
4. **Crea o revisa `.dockerignore`** en la carpeta del servicio con: `target/`, `.git/`, `.idea/`, `.vscode/`, `*.log`, `.env`.
5. **Crea el `Dockerfile`** a partir de `references/Dockerfile.template`:
   - Etapa `build`: copia `mvnw`, `pom.xml` y `.mvn`, luego `dependency:go-offline` para aprovechar la caché y después el código fuente. Compila con `package -DskipTests`; las pruebas las corre `verify` fuera de la imagen.
   - Etapa `runtime`: JRE, `curl` instalado sin recomendados y limpieza de listas en la misma capa, usuario de sistema `app`, `WORKDIR /app`, copia solo el jar final (sin `.original`), `EXPOSE 8080`, `HEALTHCHECK` con `curl -fsS http://localhost:8080/actuator/health`, y `ENTRYPOINT` con `-XX:MaxRAMPercentage=75`.
6. **Compose.** Añade a `docker-compose.yml` (raíz) solo los bloques del servicio y de su base. La base lleva `healthcheck` con `pg_isready`, y el servicio `depends_on` con `condition: service_healthy` solo de su propia base. Los secretos se leen como `${VAR}` sin valor por defecto. Declara la red `mesa-ayuda` si no existe.
7. **`.env.example`.** Añade solo las variables nuevas, con placeholders. Nunca valores reales ni defaults de secretos. No versiones `.env`.
8. **Validar** (en este orden; si una herramienta no está instalada, márcala como **pendiente**):
   - `./mvnw -B verify` en la carpeta del servicio → `BUILD SUCCESS`.
   - `docker compose config -q` → sin errores.
   - `docker build -t {{SERVICIO}}:{{VERSION}} {{SERVICIO}}`.
   - `docker run --rm --entrypoint sh {{SERVICIO}}:{{VERSION}} -c "ls -A /app"` → solo `app.jar`.
   - `docker run --rm --entrypoint whoami {{SERVICIO}}:{{VERSION}}` → `app`.
   - `docker compose up -d {{DB_SERVICIO}} {{SERVICIO}}` → ambos `healthy`; `curl -fsS http://localhost:{{PUERTO_HOST}}/actuator/health` → `{"status":"UP"}`. Luego `docker compose down`.
   - Si están instalados: `hadolint {{SERVICIO}}/Dockerfile` y `trivy image {{SERVICIO}}:{{VERSION}}` (reporta HIGH y CRITICAL).
9. **Reportar** en tabla `Archivo | Qué hace | Cómo se probó`, más la lista de pendientes con su motivo. No declares nada como validado si no lo ejecutaste.

## Prohibiciones

- No pongas `latest` como tag de la imagen base ni de la imagen final.
- No uses `ENV` ni `ARG` con contraseñas, tokens o claves.
- No copies `.env`, `.git` ni llaves al contexto de build.
- No desactives ni excluyas analizadores (`-Ddependency-check.skip` solo como bloqueo documentado).
- No hagas `git add`, `git commit`, `git push` ni crees tags; sugiere los comandos al usuario.
