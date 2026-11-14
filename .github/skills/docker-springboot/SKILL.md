---
name: docker-springboot
description: Conteneriza un microservicio Spring Boot de este repositorio siguiendo las convenciones de Java 17, Docker y Compose.
---

# Contenerizar un servicio Spring Boot

Usa esta skill solo para el servicio seleccionado y los archivos compartidos de la raíz
`docker-compose.yml` y `.env.example`. No modifiques otros servicios, la lógica de la
aplicación, los contratos de API ni las dependencias de Maven.

## Requisitos del repositorio

- Sigue `ARCHITECTURE.md` §§1.3, 5.1, 6.4 y 7, además del mapeo de servicios en
  `docs/plan-contenedores.md`.
- Usa Java 17 y una compilación multietapa con `eclipse-temurin:17-jdk` y
  `eclipse-temurin:17-jre`. No uses etiquetas flotantes como `latest`.
- Usa la versión publicada indicada en el `pom.xml` del servicio para etiquetar la
  imagen. Si no se puede leer o termina en `-SNAPSHOT`, detente y pregunta antes de
  continuar.
- El archivo `mvnw` versionado debe tener el modo `100755`. Si no lo tiene, detente y
  pregunta.
- Usa la carpeta del servicio como contexto de compilación de Docker. Su
  `.dockerignore` debe contener exactamente:

  ```text
  target/
  .git/
  .idea/
  .vscode/
  *.log
  .env
  ```

- En la etapa de compilación, copia `mvnw`, `pom.xml` y `.mvn`; descarga las
  dependencias sin conexión y luego copia `config/` y `src/` solo si esas carpetas
  existen. Empaqueta con `./mvnw -B -q package -DskipTests`.
- La etapa de ejecución debe instalar `curl` con `apt-get install
  --no-install-recommends` y eliminar las listas de apt en la misma capa. Ejecuta el
  contenedor con un usuario de sistema no root llamado `app`; copia a `/app/app.jar`
  únicamente el JAR compilado, excluyendo `*.jar.original`.
- Expón el puerto `8080`. La comprobación de salud debe llamar únicamente a
  `http://localhost:8080/actuator/health`. Conserva el punto de entrada de Java y el
  ajuste de memoria indicados en `references/Dockerfile.template`.
- No agregues credenciales a `ENV` o `ARG` en el Dockerfile; tampoco copies `.env`,
  `.git` ni claves a la imagen, ni expongas endpoints de Actuator aparte de
  `/actuator/health`.
- En Compose, agrega solo el servicio seleccionado y su base de datos. Usa
  `postgres:16`, la red `mesa-ayuda`, los nombres de servicios y bases de datos y los
  puertos del host documentados, además de una comprobación de salud con
  `pg_isready`. Haz que el servicio dependa únicamente de su base de datos mediante
  `condition: service_healthy`.
- Compose lee el `.env` de la raíz; no carga automáticamente un `.env` dentro del
  servicio. Mantén `.env` sin versionar. Agrega a `.env.example` solo marcadores de
  posición para las variables del servicio seleccionado; en YAML, las sustituciones
  de secretos deben ser `${VAR}` sin valor predeterminado. Si falta una variable o un
  secreto requerido, detente y pregunta. Nunca imprimas ni copies valores de `.env`.

## Validación

Ejecuta `./mvnw -B verify` en el servicio, `docker compose config -q` y las
comprobaciones de compilación y ejecución de Docker solicitadas para la tarea.
Confirma que la imagen contiene solo `app.jar` en `/app`, se ejecuta como `app` y
reporta estado saludable en el puerto del host asignado al servicio. Ejecuta
`hadolint` y `trivy` si están instalados; informa como pendientes las comprobaciones
que no estén disponibles y ejecuta siempre `docker compose down` después de validar
Compose.

Usa `references/Dockerfile.template` como punto de partida y omite `COPY config` o
`COPY src` solo si la carpeta correspondiente no existe.
