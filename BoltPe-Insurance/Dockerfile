
# ================================
# Stage 1: Build the Spring Boot application
# ================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven configuration first for better Docker layer caching
COPY services/pp-insurance-service/pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY services/pp-insurance-service/src ./src

# Build application
RUN mvn clean package -DskipTests


# ================================
# Stage 2: Run the application
# ================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy generated JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 3007

ENTRYPOINT ["java", "-jar", "app.jar"]
