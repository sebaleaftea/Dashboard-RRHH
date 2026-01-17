# Script para ejecutar el ETL localmente y ver los logs en tiempo real
# Esto te permitira ver exactamente que esta devolviendo la API de Talana

Write-Host "=== Ejecutando ETL localmente para debug ===" -ForegroundColor Green
Write-Host ""

# Configurar variables de entorno
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "grh"
$env:DB_USER = "grh-bdduser"
$env:DB_PASS = 'c_}2ysUR"6dXEk]o'

# IMPORTANTE: Necesitas configurar el token de Talana
Write-Host "IMPORTANTE: Debes configurar tu token de Talana" -ForegroundColor Yellow
$talanatokenInput = Read-Host "Ingresa el token de Talana API (o presiona Enter para usar variable de entorno TALANA_TOKEN)"
if ($talanatokenInput) {
    $env:TALANA_TOKEN = $talanatokenInput
}

# URL de la API de Talana - ajusta segun tu endpoint
$talanaUrlInput = Read-Host "Ingresa la URL de Talana para contratos (o presiona Enter para usar por defecto)"
if ($talanaUrlInput) {
    $env:TALANA_URL = $talanaUrlInput
} else {
    # URL por defecto - ajusta segun tu configuracion
    $env:TALANA_URL = "https://talana.com/es/api/contracts"
}

Write-Host ""
Write-Host "Configuracion:" -ForegroundColor Cyan
Write-Host "  DB: $env:DB_HOST`:$env:DB_PORT/$env:DB_NAME"
Write-Host "  User: $env:DB_USER"
Write-Host "  Talana URL: $env:TALANA_URL"
Write-Host "  Token configurado: $(if($env:TALANA_TOKEN){'SI'}else{'NO'})"
Write-Host ""

if (-not $env:TALANA_TOKEN) {
    Write-Host "ERROR: No se configuro el token de Talana. Configuralo con:" -ForegroundColor Red
    Write-Host '  $env:TALANA_TOKEN = "tu-token-aqui"' -ForegroundColor Yellow
    exit 1
}

# Verificar si Python esta instalado
try {
    $pythonVersion = python --version 2>&1
    Write-Host "Python encontrado: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Python no esta instalado o no esta en el PATH" -ForegroundColor Red
    exit 1
}

# Verificar e instalar dependencias
Write-Host ""
Write-Host "Verificando dependencias de Python..." -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\etl"

if (-not (Test-Path "venv")) {
    Write-Host "Creando entorno virtual..." -ForegroundColor Yellow
    python -m venv venv
}

Write-Host "Activando entorno virtual..." -ForegroundColor Yellow
.\venv\Scripts\Activate.ps1

Write-Host "Instalando dependencias..." -ForegroundColor Yellow
pip install -r requirements.txt --quiet

# Ejecutar el ETL
Write-Host ""
Write-Host "=== EJECUTANDO ETL - Los logs apareceran a continuacion ===" -ForegroundColor Green
Write-Host ""

python etl.py

$exitCode = $LASTEXITCODE

# Desactivar entorno virtual
deactivate

Pop-Location

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "=== ETL COMPLETADO EXITOSAMENTE ===" -ForegroundColor Green
} else {
    Write-Host "=== ETL FALLO - Revisa los errores arriba ===" -ForegroundColor Red
}

Write-Host ""
Write-Host "Los logs mostraron:" -ForegroundColor Cyan
Write-Host "  1. La estructura del primer registro de Talana"
Write-Host "  2. Cuantos empleados/contratos se procesaron"
Write-Host "  3. Cualquier error que haya ocurrido"
Write-Host ""
Write-Host "Ahora puedes verificar la BD con:" -ForegroundColor Yellow
Write-Host "  SELECT rut, nombre, discapacidad FROM empleado WHERE discapacidad IS NOT NULL;"
