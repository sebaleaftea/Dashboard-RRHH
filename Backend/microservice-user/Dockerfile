# Multi-stage build para optimizar tamaño de imagen

# Stage 1: Build con Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copiar archivos de configuración de Maven primero (para cachear dependencias)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar aplicación (skip tests para deploy más rápido)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime con JRE (más ligero que JDK)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Agregar usuario no-root para seguridad
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring
USER spring:spring

# Copiar JAR desde stage de build
COPY --from=builder /build/target/*.jar app.jar

# Variables de entorno por defecto (se sobreescriben en Cloud Run)
ENV SPRING_PROFILES_ACTIVE=cloudrun
ENV SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Healthcheck para Cloud Run
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Ejecutar aplicación con optimizaciones JVM
ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
