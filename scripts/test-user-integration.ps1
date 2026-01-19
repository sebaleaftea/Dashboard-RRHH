# Script de prueba para verificar la integración microservice-user + frontend

Write-Host "=== Test de Integración Usuario ===" -ForegroundColor Cyan
Write-Host ""

# Verificar que el microservicio esté corriendo
Write-Host "1. Verificando microservice-user en puerto 8081..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/user/all" -UseBasicParsing -ErrorAction Stop
    Write-Host "   ✓ Microservicio respondiendo correctamente" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Error: Microservicio no disponible" -ForegroundColor Red
    Write-Host "   Ejecuta primero: .\run-user-cloudsql.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Listar usuarios existentes
Write-Host "2. Listando usuarios en la base de datos..." -ForegroundColor Yellow
try {
    $usersResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/user/all" -UseBasicParsing
    $users = $usersResponse.Content | ConvertFrom-Json
    
    if ($users.Count -eq 0) {
        Write-Host "   ! No hay usuarios en la base de datos" -ForegroundColor Yellow
        Write-Host "   Asegúrate de haber insertado usuarios en la tabla 'usuarios'" -ForegroundColor Yellow
    } else {
        Write-Host "   ✓ Usuarios encontrados: $($users.Count)" -ForegroundColor Green
        foreach ($user in $users) {
            Write-Host "     - Username: $($user.username), Nombre: $($user.nombre), Rol: $($user.rol), Activo: $($user.activo)" -ForegroundColor Cyan
        }
    }
} catch {
    Write-Host "   ✗ Error al listar usuarios: $_" -ForegroundColor Red
}

Write-Host ""

# Probar login con credenciales de prueba
Write-Host "3. Probando endpoint de login..." -ForegroundColor Yellow
$testCredentials = @{
    username = "admin"
    password = "admin123"
}

$body = $testCredentials | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/user/login" `
        -Method POST `
        -Body $body `
        -ContentType "application/json" `
        -UseBasicParsing
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "   ✓ Login exitoso!" -ForegroundColor Green
    Write-Host "     - Username: $($loginData.username)" -ForegroundColor Cyan
    Write-Host "     - Nombre: $($loginData.nombre)" -ForegroundColor Cyan
    Write-Host "     - Email: $($loginData.email)" -ForegroundColor Cyan
    Write-Host "     - Rol: $($loginData.rol)" -ForegroundColor Cyan
    Write-Host "     - Último acceso: $($loginData.ultimoAcceso)" -ForegroundColor Cyan
} catch {
    Write-Host "   ✗ Login falló (esto es esperado si no existe el usuario 'admin')" -ForegroundColor Yellow
    Write-Host "   Mensaje: $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Test Completado ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Nota: Para probar el frontend, ejecuta:" -ForegroundColor Yellow
Write-Host "  cd ..\Front\dashboard-rrhh" -ForegroundColor Gray
Write-Host "  npm run dev" -ForegroundColor Gray
Write-Host ""
Write-Host "Luego accede a http://localhost:5173 y usa las credenciales:" -ForegroundColor Yellow
Write-Host "  Username: admin" -ForegroundColor Gray
Write-Host "  Password: admin123" -ForegroundColor Gray
