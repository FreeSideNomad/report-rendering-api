# Use Red Hat Enterprise Linux 9 as base image
FROM registry.access.redhat.com/ubi9/ubi:latest

# Set maintainer label
LABEL maintainer="igormusic@tvmsoftware.com"
LABEL description="Spring Boot Report Rendering API with Playwright"

# Install required packages
RUN dnf update -y && \
    dnf install -y \
    java-17-openjdk-devel \
    maven \
    wget \
    curl \
    ca-certificates \
    fontconfig \
    libX11 \
    libXcomposite \
    libXdamage \
    libXext \
    libXfixes \
    libXrandr \
    libXrender \
    libXtst \
    libxcb \
    libxshmfence \
    libnss3 \
    libdrm \
    libxkbcommon \
    libatspi \
    libgtk-3 \
    alsa-lib && \
    dnf clean all

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

# Create application directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Install Node.js (required for Playwright)
RUN curl -fsSL https://rpm.nodesource.com/setup_20.x | bash - && \
    dnf install -y nodejs

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create directories and set permissions
RUN mkdir -p /app/logs /app/playwright && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Install Playwright and Chromium
RUN npm init -y && \
    npm install playwright@latest && \
    npx playwright install chromium && \
    npx playwright install-deps chromium

# Set environment variables for Playwright
ENV PLAYWRIGHT_BROWSERS_PATH=/app/playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=false

# Expose port
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/templates || exit 1

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/report-rendering-api-*.jar"]