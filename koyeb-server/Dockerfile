# ==============================================================================
# AcadsCatchUp — Gmail & Email Checker Server (Koyeb Deployment)
# 100% Pure Java 21 • Zero External Dependencies • Ultra-Fast Alpine Container
# Developer: F4TAL
# ==============================================================================

# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY src ./src
RUN mkdir -p bin && javac -d bin src/com/acadscatchup/server/GmailCheckerServer.java

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy compiled bytecode from builder
COPY --from=builder /app/bin ./bin

# Koyeb injects the PORT environment variable (default: 8000)
ENV PORT=8000
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/health || exit 1

# Start the embedded Java HTTP Server
CMD ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-cp", "bin", "com.acadscatchup.server.GmailCheckerServer"]
