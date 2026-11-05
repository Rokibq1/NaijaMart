FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for container health checks
RUN apt-get update 
    && apt-get install -y --no-install-recommends curl 
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Allow Spring Boot time to initialize
HEALTHCHECK --interval=30s --timeout=10s --start-period=300s --retries=10 
    CMD curl -fsS http://localhost:8080/ || exit 1

# Limit Java memory usage
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=55.0", "-XX:InitialRAMPercentage=20.0", "-XX:+UseSerialGC", "-jar", "app.jar"]