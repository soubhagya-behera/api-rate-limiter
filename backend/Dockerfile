# ---- Build stage: compile the Spring Boot fat JAR with Maven (Java 17) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies: copy the POM first and download plugins/dependencies.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy sources and package (tests are run separately; skip here for faster image builds).
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage: JRE-only image with just the Spring Boot JAR ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/api-rate-limiter-*.jar app.jar

EXPOSE 8080

# Redis host/port come from environment (SPRING_DATA_REDIS_HOST / SPRING_DATA_REDIS_PORT).
ENTRYPOINT ["java", "-jar", "app.jar"]
