# Script para ejecutar microservice-user conectado a Cloud SQL PostgreSQL
# Asegúrate de tener Cloud SQL Proxy corriendo en el puerto 5433

Write-Host "=== Ejecutando microservice-user con Cloud SQL ===" -ForegroundColor Cyan

# Navegar al directorio del microservicio
Set-Location "c:\Workspace\Dashboard RRHH\Backend\microservice-user"

# Configurar variables de entorno
$env:SPRING_PROFILES_ACTIVE = "cloudsql"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "grh"
$env:DB_USERNAME = "grh-bdduser"
$env:DB_PASSWORD = 'c_}2ysUR"6dXEk]o'

Write-Host "Perfil activo: cloudsql" -ForegroundColor Green
Write-Host "Base de datos: grh (PostgreSQL vía Cloud SQL Proxy)" -ForegroundColor Green
Write-Host ""

# Ejecutar con Maven
.\mvnw spring-boot:run
