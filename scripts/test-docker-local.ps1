# Script para probar Docker localmente antes de desplegar a Cloud Run

param(
    [Parameter(Mandatory=$false)]
    [int]$Port = 8081
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Local de Docker - microservice-user" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$microservicePath = Join-Path $PSScriptRoot "..\Backend\microservice-user"
Set-Location $microservicePath

# Verificar que Cloud SQL Proxy esté corriendo
Write-Host "1. Verificando Cloud SQL Proxy..." -ForegroundColor Green
$proxyProcess = Get-Process | Where-Object {$_.ProcessName -eq "cloud-sql-proxy" -or $_.ProcessName -eq "cloud_sql_proxy"}
if (-not $proxyProcess) {
    Write-Host "   ✗ Cloud SQL Proxy no está corriendo" -ForegroundColor Red
    Write-Host "   Inicia el proxy primero:" -ForegroundColor Yellow
    Write-Host "   .\scripts\start-cloud-sql-proxy.ps1" -ForegroundColor Gray
    exit 1
}
Write-Host "   ✓ Cloud SQL Proxy corriendo (PID: $($proxyProcess.Id))" -ForegroundColor Green

# Compilar con Maven
Write-Host ""
Write-Host "2. Compilando aplicación..." -ForegroundColor Green
.\mvnw clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error en compilación" -ForegroundColor Red
    exit 1
}
Write-Host "   ✓ Compilación exitosa" -ForegroundColor Green

# Build imagen Docker
Write-Host ""
Write-Host "3. Construyendo imagen Docker..." -ForegroundColor Green
docker build -t microservice-user:local .
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error al construir imagen" -ForegroundColor Red
    exit 1
}
Write-Host "   ✓ Imagen construida: microservice-user:local" -ForegroundColor Green

# Detener contenedor anterior si existe
Write-Host ""
Write-Host "4. Limpiando contenedores anteriores..." -ForegroundColor Green
$existingContainer = docker ps -a --filter "name=microservice-user-test" --format "{{.ID}}"
if ($existingContainer) {
    docker rm -f microservice-user-test | Out-Null
    Write-Host "   ✓ Contenedor anterior eliminado" -ForegroundColor Green
} else {
    Write-Host "   → No hay contenedores anteriores" -ForegroundColor Gray
}

# Ejecutar contenedor
Write-Host ""
Write-Host "5. Ejecutando contenedor Docker..." -ForegroundColor Green
Write-Host "   Puerto: $Port" -ForegroundColor Gray
Write-Host "   Conectando a Cloud SQL vía proxy (localhost:5433)" -ForegroundColor Gray

docker run -d `
  --name microservice-user-test `
  --network="host" `
  -p ${Port}:8080 `
  -e SPRING_PROFILES_ACTIVE=cloudsql `
  -e DB_HOST=localhost `
  -e DB_PORT=5433 `
  -e DB_NAME=grh `
  -e DB_USERNAME=grh-bdduser `
  -e "DB_PASSWORD=c_}2ysUR`"6dXEk]o" `
  microservice-user:local

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error al ejecutar contenedor" -ForegroundColor Red
    exit 1
}

Write-Host "   ✓ Contenedor iniciado" -ForegroundColor Green
Write-Host ""
Write-Host "   Esperando que el servicio inicie..." -ForegroundColor Gray

# Esperar a que el servicio esté listo
$maxRetries = 30
$retryCount = 0
$serviceReady = $false

while ($retryCount -lt $maxRetries -and -not $serviceReady) {
    Start-Sleep -Seconds 2
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $serviceReady = $true
        }
    } catch {
        $retryCount++
        Write-Host "   → Intento $retryCount/$maxRetries..." -ForegroundColor Gray
    }
}

if ($serviceReady) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  ✓ Servicio Docker corriendo!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "URLs disponibles:" -ForegroundColor Yellow
    Write-Host "  - Health: http://localhost:$Port/actuator/health" -ForegroundColor Gray
    Write-Host "  - Users:  http://localhost:$Port/api/user/all" -ForegroundColor Gray
    Write-Host "  - Login:  http://localhost:$Port/api/user/login" -ForegroundColor Gray
    Write-Host ""
    
    # Probar endpoint
    Write-Host "6. Probando endpoint /api/user/all..." -ForegroundColor Green
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port/api/user/all" -UseBasicParsing
        Write-Host "   ✓ Respuesta exitosa (Status: $($response.StatusCode))" -ForegroundColor Green
        Write-Host ""
        Write-Host "Usuarios encontrados:" -ForegroundColor White
        $users = $response.Content | ConvertFrom-Json
        foreach ($user in $users) {
            Write-Host "  - $($user.username) ($($user.nombre)) - Rol: $($user.rol)" -ForegroundColor Cyan
        }
    } catch {
        Write-Host "   ⚠ Error al probar endpoint: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Comandos útiles:" -ForegroundColor White
    Write-Host "  Ver logs:    docker logs -f microservice-user-test" -ForegroundColor Gray
    Write-Host "  Detener:     docker stop microservice-user-test" -ForegroundColor Gray
    Write-Host "  Eliminar:    docker rm -f microservice-user-test" -ForegroundColor Gray
    Write-Host "  Inspeccionar: docker inspect microservice-user-test" -ForegroundColor Gray
    Write-Host ""
    
} else {
    Write-Host ""
    Write-Host "   ✗ El servicio no respondió a tiempo" -ForegroundColor Red
    Write-Host ""
    Write-Host "Ver logs del contenedor:" -ForegroundColor Yellow
    Write-Host "  docker logs microservice-user-test" -ForegroundColor Gray
    Write-Host ""
    
    # Mostrar logs
    Write-Host "Últimas líneas del log:" -ForegroundColor Yellow
    docker logs --tail 20 microservice-user-test
    
    exit 1
}
