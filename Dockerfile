# syntax=docker/dockerfile:1

# ---------- Build stage: compiles the application with the JDK ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# The cache mount keeps downloaded dependencies between builds, so rebuilds are fast.
# Tests run in the CI, not while building the image.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -Dmaven.test.skip=true

# ---------- Runtime stage: only the JRE and the jar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Do not run the application as root
RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser
USER appuser

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
