# Stage 1: Build with Maven + JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime with lightweight JDK 17
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/civic-issue-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
