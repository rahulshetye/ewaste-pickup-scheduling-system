# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package -Dmaven.test.skip=true \
    && cp target/ewaste-pickup-*.jar /build/app.jar

# ---------- Stage 2: runtime image ----------
FROM eclipse-temurin:17-jre
LABEL org.opencontainers.image.title="ewaste-pickup" \
      org.opencontainers.image.description="E-Waste Pickup Scheduling System (Spring Boot)"
RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER appuser
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
