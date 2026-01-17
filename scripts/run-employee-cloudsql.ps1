param(
  [Parameter(Mandatory=$true)][string]$DbHost,
  [string]$DbPort = "5432",
  [string]$DbName = "grh",
  [string]$DbUser = "grh-bdduser",
  [switch]$DisableFlyway
)

Write-Host "Preparing to connect to Cloud SQL at $DbHost:$DbPort for DB '$DbName'..."
$securePwd = Read-Host "Enter DB password for $DbUser" -AsSecureString
$plainPwd = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePwd))

Write-Host "Checking TCP connectivity to $DbHost:$DbPort..."
$result = Test-NetConnection -ComputerName $DbHost -Port $DbPort
if (-not $result.TcpTestSucceeded) {
  Write-Error "Cannot reach $DbHost:$DbPort. Ensure your IP is whitelisted in Cloud SQL Authorized Networks and port 5432 is open."
  exit 1
}

$env:SPRING_PROFILES_ACTIVE = "cloudsql"
$env:DB_USERNAME = $DbUser
$env:DB_PASSWORD = $plainPwd
$env:DB_HOST = $DbHost
$env:DB_PORT = $DbPort
$env:DB_NAME = $DbName

if ($DisableFlyway) {
  Write-Host "Disabling Flyway via SPRING_FLYWAY_ENABLED=false"
  $env:SPRING_FLYWAY_ENABLED = "false"
}

Write-Host "Starting microservice-employee with 'cloudsql' profile..."
Push-Location "c:\Workspace\Dashboard RRHH\Backend\microservice-employee"
./mvnw.cmd -DskipTests spring-boot:run
Pop-Location
