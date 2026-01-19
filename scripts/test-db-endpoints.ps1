# Test DB Endpoints
# Ejecutar después de iniciar el microservice-employee

$baseUrl = "http://localhost:8082/api/db"

Write-Host "`n===== Probando Endpoints de Base de Datos =====`n" -ForegroundColor Cyan

# Test 1: Stats generales
Write-Host "1. GET /stats" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/stats" -Method Get
    $response | ConvertTo-Json
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 2: Empleados activos
Write-Host "`n2. GET /empleados/activos (primeros 3)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/empleados/activos" -Method Get
    $response | Select-Object -First 3 | ConvertTo-Json
    Write-Host "Total empleados activos: $($response.Count)" -ForegroundColor Green
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 3: Contratos activos
Write-Host "`n3. GET /contratos/activos (primeros 3)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/contratos/activos" -Method Get
    $response | Select-Object -First 3 | ConvertTo-Json
    Write-Host "Total contratos activos: $($response.Count)" -ForegroundColor Green
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 4: Detalle de un empleado
Write-Host "`n4. GET /empleados/1/detalle" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/empleados/1/detalle" -Method Get
    $response | ConvertTo-Json
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 5: Vacaciones de un empleado
Write-Host "`n5. GET /empleados/1/vacaciones" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/empleados/1/vacaciones" -Method Get
    $response | Select-Object -First 3 | ConvertTo-Json
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 6: Licencias de un empleado
Write-Host "`n6. GET /empleados/1/licencias" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/empleados/1/licencias" -Method Get
    $response | Select-Object -First 3 | ConvertTo-Json
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 7: Métricas diarias de vacaciones
Write-Host "`n7. GET /metrics/vacaciones/daily?days=7" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/metrics/vacaciones/daily?days=7" -Method Get
    $response | ConvertTo-Json
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 8: Métricas diarias de licencias
Write-Host "`n8. GET /metrics/licencias/daily?days=7" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/metrics/licencias/daily?days=7" -Method Get
    $response | ConvertTo-Json
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "`n===== Pruebas Completadas =====`n" -ForegroundColor Cyan
