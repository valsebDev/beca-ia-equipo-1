# Imagen de build: JDK 17 con Maven wrapper, fijada a la versión 17.0.21_9
FROM eclipse-temurin:17.0.21_9-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY config config
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Imagen de ejecución: JRE 17 con usuario no root y healthcheck de Actuator
FROM eclipse-temurin:17.0.21_9-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl=8.18.0-1ubuntu2.8 \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/*.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
