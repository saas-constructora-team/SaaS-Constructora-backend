# ===============================================================
# Dockerfile — Constructora backend (Spring Boot 4.1.1, JDK 25)
# Multi-stage: build (Maven) -> runtime (JRE, usuario no-root)
# ===============================================================

# ---------------------------------------------------------------
# Stage 1: build
# ---------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# Cachea dependencias: solo se invalidan si cambia el pom
COPY --chmod=755 mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp package -DskipTests

# ---------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

# Usuario sin privilegios
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=builder /build/target/Constructora-0.1.0-SNAPSHOT.jar app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
