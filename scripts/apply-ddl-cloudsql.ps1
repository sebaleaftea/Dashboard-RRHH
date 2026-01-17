param(
    [string]$Project = 'prd-sfh-it-bi-erbi',
    [string]$Instance = 'bdd-grh',
    [string]$Database = 'grh',
    [string]$User = 'grh-bdduser'
)

Write-Host "=== Aplicar DDL a Cloud SQL ===" -ForegroundColor Cyan
Write-Host "Proyecto: $Project"
Write-Host "Instancia: $Instance"
Write-Host "Base de datos: $Database"
Write-Host "Usuario: $User"
Write-Host ""

$ddlFile = "$PSScriptRoot\apply-ddl.sql"
if (-not (Test-Path $ddlFile)) {
    Write-Error "No se encuentra el archivo DDL: $ddlFile"
    exit 1
}

Write-Host "Opción 1: Usar gcloud (recomendado)" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "Ejecuta este comando en tu terminal:" -ForegroundColor Green
Write-Host ""
$gcloudCmd = "gcloud sql connect $Instance --user=$User --database=$Database --project=$Project --quiet"
Write-Host $gcloudCmd -ForegroundColor White
Write-Host ""
Write-Host "Luego copia y pega el contenido del archivo:" -ForegroundColor Green
Write-Host "  $ddlFile" -ForegroundColor White
Write-Host ""
Write-Host "Contraseña: c_}2ysUR`"6dXEk]o" -ForegroundColor Magenta
Write-Host ""

Write-Host "Opción 2: Usar psql local (si está instalado)" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
$env:PGPASSWORD = 'c_}2ysUR"6dXEk]o'
$psqlCmd = "psql -h 34.176.82.205 -U $User -d $Database -f `"$ddlFile`""
Write-Host $psqlCmd -ForegroundColor White
Write-Host ""

$choice = Read-Host "¿Intentar con psql local? (s/N)"
if ($choice -eq 's' -or $choice -eq 'S') {
    # Verificar si psql está disponible
    $psqlPath = Get-Command psql -ErrorAction SilentlyContinue
    if ($null -eq $psqlPath) {
        Write-Error "psql no está instalado. Usa la Opción 1 (gcloud) en su lugar."
        Write-Host ""
        Write-Host "Para instalar PostgreSQL client:"
        Write-Host "  winget install PostgreSQL.PostgreSQL"
        exit 1
    }
    
    Write-Host "Ejecutando DDL con psql..." -ForegroundColor Green
    & psql -h 34.176.82.205 -U $User -d $Database -f $ddlFile
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "DDL aplicado exitosamente" -ForegroundColor Green
        Write-Host ""
        Write-Host "Verificando tablas creadas..."
        $verifyQuery = "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;"
        echo $verifyQuery | & psql -h 34.176.82.205 -U $User -d $Database
    } else {
        Write-Host "Error al aplicar DDL. Codigo de salida: $LASTEXITCODE" -ForegroundColor Red
    }
} else {
    Write-Host ""
    Write-Host "Copiando contenido del DDL al portapapeles..." -ForegroundColor Cyan
    Get-Content $ddlFile | Set-Clipboard
    Write-Host "DDL copiado al portapapeles" -ForegroundColor Green
    Write-Host ""
    Write-Host "Pasos siguientes:" -ForegroundColor Yellow
    Write-Host "1. Ejecuta el comando gcloud de arriba"
    Write-Host "2. Ingresa la contraseña: c_}2ysUR`"6dXEk]o"
    Write-Host "3. Pega el DDL (Ctrl+V) y presiona Enter"
    Write-Host "4. Verifica con: \dt"
}
