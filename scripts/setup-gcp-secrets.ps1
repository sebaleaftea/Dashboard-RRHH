# Script para configurar secrets en Google Cloud Secret Manager
# Ejecutar ANTES del primer despliegue

param(
    [Parameter(Mandatory=$false)]
    [string]$ProjectId = "prd-sfh-it-bi-erbi"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Configuracion de Secrets en GCP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configurar proyecto
Write-Host "1. Configurando proyecto: $ProjectId" -ForegroundColor Green
gcloud config set project $ProjectId
if ($LASTEXITCODE -ne 0) {
    Write-Host "   Error al configurar proyecto" -ForegroundColor Red
    exit 1
}
Write-Host "   Proyecto configurado exitosamente" -ForegroundColor Green

# Habilitar Secret Manager API
Write-Host ""
Write-Host "2. Habilitando Secret Manager API..." -ForegroundColor Green
gcloud services enable secretmanager.googleapis.com
if ($LASTEXITCODE -ne 0) {
    Write-Host "   Error al habilitar API" -ForegroundColor Red
    exit 1
}
Write-Host "   API habilitada exitosamente" -ForegroundColor Green

# Crear secret para la contraseña de la base de datos
Write-Host ""
Write-Host "3. Creando secret para contrasena de DB..." -ForegroundColor Green

$dbPassword = 'c_}2ysUR"6dXEk]o'

# Verificar si el secret ya existe
$existingSecret = gcloud secrets describe grh-db-password 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "   Advertencia: El secret 'grh-db-password' ya existe" -ForegroundColor Yellow
    $overwrite = Read-Host "   Deseas actualizarlo? (S/N)"
    if ($overwrite -eq "S" -or $overwrite -eq "s") {
        echo $dbPassword | gcloud secrets versions add grh-db-password --data-file=-
        Write-Host "   Secret actualizado exitosamente" -ForegroundColor Green
    } else {
        Write-Host "   Usando secret existente" -ForegroundColor Gray
    }
} else {
    # Crear nuevo secret
    echo $dbPassword | gcloud secrets create grh-db-password `
        --data-file=- `
        --replication-policy="automatic"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "   Error al crear secret" -ForegroundColor Red
        exit 1
    }
    Write-Host "   Secret creado exitosamente: grh-db-password" -ForegroundColor Green
}

# Obtener el número del proyecto para la service account
Write-Host ""
Write-Host "4. Configurando permisos..." -ForegroundColor Green
$projectNumber = gcloud projects describe $ProjectId --format="value(projectNumber)"
$serviceAccount = "$projectNumber-compute@developer.gserviceaccount.com"

Write-Host "   Service Account: $serviceAccount" -ForegroundColor Gray

# Dar permiso a Cloud Run para acceder al secret
gcloud secrets add-iam-policy-binding grh-db-password `
    --member="serviceAccount:$serviceAccount" `
    --role="roles/secretmanager.secretAccessor"

if ($LASTEXITCODE -ne 0) {
    Write-Host "   Error al configurar permisos" -ForegroundColor Red
    exit 1
}
Write-Host "   Permisos configurados exitosamente" -ForegroundColor Green

# Verificar secrets
Write-Host ""
Write-Host "5. Verificando secrets creados..." -ForegroundColor Green
gcloud secrets list
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Configuracion Completada" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Secrets disponibles:" -ForegroundColor Yellow
Write-Host "  - grh-db-password: Contrasena de Cloud SQL" -ForegroundColor Gray
Write-Host ""
Write-Host "Ahora puedes ejecutar el despliegue:" -ForegroundColor White
Write-Host "  .\deploy-user-cloudrun.ps1" -ForegroundColor Gray
Write-Host ""
