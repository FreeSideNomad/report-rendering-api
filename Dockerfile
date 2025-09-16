

# ---- Build Stage ----
FROM openjdk:17-slim AS build

WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Runtime Stage ----
FROM mcr.microsoft.com/playwright:v1.50.0-noble

# Set maintainer label
LABEL maintainer="igormusic@tvmsoftware.com"
LABEL description="Spring Boot Report Rendering API with Playwright"



# Install minimal Java runtime
USER root
RUN apt-get update && \
    apt-get install -y openjdk-17-jre && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"


# Create application directory
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/report-rendering-api-*.jar ./target/



# Set permissions for pwuser (default Playwright user)
RUN mkdir -p /app/logs && \
    chown -R pwuser:pwuser /app

USER pwuser



# Expose port
EXPOSE 8080



# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"



# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1



# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/report-rendering-api-*.jar"]