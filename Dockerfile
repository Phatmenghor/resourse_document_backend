# =========================
# Build stage
# =========================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first → cache dependencies
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -T 1C


# =========================
# Runtime stage (small image)
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# ═════════════════════════════════════════════════════════════
# LOGGING CONFIGURATION
# ═════════════════════════════════════════════════════════════
# Create log directories for:
# - /DATA/resource_center (main logs directory)
# - Archive subdirectory for compressed logs
# ═════════════════════════════════════════════════════════════
RUN mkdir -p /DATA/resource_center/archive && \
    chmod 755 /DATA/resource_center && \
    chmod 755 /DATA/resource_center/archive

# Create local logs directory as fallback
RUN mkdir -p /app/logs && chmod 755 /app/logs

EXPOSE 8080

# Run with LOG_PATH environment variable pointing to /DATA/resource_center
# Override in docker-compose.yml or runtime if needed
ENV LOG_PATH=/DATA/resource_center
ENV JAVA_OPTS=-Dlogging.config=classpath:logback-spring.xml

ENTRYPOINT ["java", "-jar", "app.jar"]
