# Script de despliegue automatizado para microservice-user en Cloud Run

param(
    [Parameter(Mandatory=$false)]
    [string]$ProjectId = "prd-sfh-it-bi-erbi",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "southamerica-west1",
    
    [Parameter(Mandatory=$false)]
    [string]$ServiceName = "microservice-user",
    
    [Parameter(Mandatory=$false)]
    [ValidateSet("staging", "production")]
    [string]$Environment = "staging"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Despliegue de microservice-user" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proyecto: $ProjectId" -ForegroundColor Yellow
Write-Host "Región: $Region" -ForegroundColor Yellow
Write-Host "Servicio: $ServiceName" -ForegroundColor Yellow
Write-Host "Ambiente: $Environment" -ForegroundColor Yellow
Write-Host ""

# Verificar que gcloud esté instalado
Write-Host "1. Verificando gcloud CLI..." -ForegroundColor Green
try {
    $gcloudVersion = gcloud version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud no está instalado"
    }
    Write-Host "   ✓ gcloud CLI encontrado" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Error: gcloud CLI no está instalado" -ForegroundColor Red
    Write-Host "   Instala desde: https://cloud.google.com/sdk/docs/install" -ForegroundColor Yellow
    exit 1
}

# Configurar proyecto
Write-Host ""
Write-Host "2. Configurando proyecto GCP..." -ForegroundColor Green
gcloud config set project $ProjectId
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error al configurar proyecto" -ForegroundColor Red
    exit 1
}
Write-Host "   ✓ Proyecto configurado" -ForegroundColor Green

# Navegar al directorio del microservicio
Write-Host ""
Write-Host "3. Navegando al directorio del microservicio..." -ForegroundColor Green
$microservicePath = Join-Path $PSScriptRoot "..\Backend\microservice-user"
Set-Location $microservicePath
Write-Host "   ✓ Directorio: $microservicePath" -ForegroundColor Green

# Compilar con Maven
Write-Host ""
Write-Host "4. Compilando aplicación con Maven..." -ForegroundColor Green
Write-Host "   (Esto puede tomar unos minutos...)" -ForegroundColor Gray
.\mvnw clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error en compilación Maven" -ForegroundColor Red
    exit 1
}
Write-Host "   ✓ Compilación exitosa" -ForegroundColor Green

# Build y push de imagen Docker a Container Registry
Write-Host ""
Write-Host "5. Construyendo y subiendo imagen Docker..." -ForegroundColor Green
Write-Host "   (Esto puede tomar varios minutos...)" -ForegroundColor Gray
$imageName = "gcr.io/$ProjectId/$ServiceName"
gcloud builds submit --tag $imageName .
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error al construir imagen Docker" -ForegroundColor Red
    exit 1
}
Write-Host "   ✓ Imagen construida: $imageName" -ForegroundColor Green

# Variables de entorno según ambiente
$dbName = "grh"
$dbUsername = "grh-bdduser"
$cloudSqlInstance = "prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh"

# Deploy a Cloud Run
Write-Host ""
Write-Host "6. Desplegando a Cloud Run..." -ForegroundColor Green
Write-Host "   (Configurando servicio...)" -ForegroundColor Gray

gcloud run deploy $ServiceName `
  --image=$imageName `
  --platform=managed `
  --region=$Region `
  --allow-unauthenticated `
  --port=8080 `
  --memory=512Mi `
  --cpu=1 `
  --timeout=300 `
  --max-instances=10 `
  --min-instances=0 `
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun,DB_NAME=$dbName,DB_USERNAME=$dbUsername,CLOUD_SQL_INSTANCE=$cloudSqlInstance" `
  --set-secrets="DB_PASSWORD=grh-db-password:latest" `
  --add-cloudsql-instances=$cloudSqlInstance `
  --service-account="$ProjectId@appspot.gserviceaccount.com"

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ✗ Error al desplegar en Cloud Run" -ForegroundColor Red
    exit 1
}

# Obtener URL del servicio
Write-Host ""
Write-Host "7. Obteniendo URL del servicio..." -ForegroundColor Green
$serviceUrl = gcloud run services describe $ServiceName --region=$Region --format="value(status.url)"
Write-Host "   ✓ Servicio desplegado exitosamente!" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  🎉 DESPLIEGUE COMPLETADO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "URL del servicio: $serviceUrl" -ForegroundColor Yellow
Write-Host ""
Write-Host "Endpoints disponibles:" -ForegroundColor White
Write-Host "  - GET  $serviceUrl/api/user/all" -ForegroundColor Gray
Write-Host "  - POST $serviceUrl/api/user/login" -ForegroundColor Gray
Write-Host "  - POST $serviceUrl/api/user/create" -ForegroundColor Gray
Write-Host ""
Write-Host "Para ver logs:" -ForegroundColor White
Write-Host "  gcloud run services logs read $ServiceName --region=$Region" -ForegroundColor Gray
Write-Host ""
Write-Host "Para ver detalles del servicio:" -ForegroundColor White
Write-Host "  gcloud run services describe $ServiceName --region=$Region" -ForegroundColor Gray
Write-Host ""

# Probar endpoint de salud
Write-Host "8. Probando servicio desplegado..." -ForegroundColor Green
Start-Sleep -Seconds 5
try {
    $response = Invoke-WebRequest -Uri "$serviceUrl/api/user/all" -UseBasicParsing -TimeoutSec 10
    Write-Host "   ✓ Servicio responde correctamente (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ⚠ Advertencia: No se pudo verificar el servicio" -ForegroundColor Yellow
    Write-Host "   Verifica manualmente: $serviceUrl/api/user/all" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
