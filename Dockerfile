FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy source code
COPY . .

# Build the project
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install runtime dependencies
RUN apk add --no-cache bash curl jq

# Create non-root user
RUN addgroup -S nanobot && adduser -S nanobot -G nanobot
USER nanobot

# Copy built artifact
COPY --from=builder /app/target/nanobot-*.jar /app/nanobot.jar

# Set working directory
WORKDIR /app

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Default command
CMD ["java", "-jar", "/app/nanobot.jar"]
