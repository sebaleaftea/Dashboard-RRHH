param(
  [string]$ConnectionName = "prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh",
  [int]$Port = 5433,
  [string]$CredentialsFile = ""  # Opcional: ruta a JSON de Service Account
)

$ProxyDir = "C:\Tools\cloud-sql-proxy"
$ProxyPath = Join-Path $ProxyDir "cloud-sql-proxy.exe"
# Descarga desde GitHub releases (última versión estable para Windows AMD64)
$Url = "https://github.com/GoogleCloudPlatform/cloud-sql-proxy/releases/latest/download/cloud-sql-proxy-windows-amd64.exe"

# Forzar TLS 1.2 para descargas
try {
  [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls11
} catch {}

if (-not (Test-Path $ProxyPath)) {
  Write-Host "Descargando Cloud SQL Auth Proxy a $ProxyPath ..."
  New-Item -ItemType Directory -Path $ProxyDir -Force | Out-Null
  $tmpPath = Join-Path $ProxyDir "cloud-sql-proxy-windows-amd64.exe"
  
  $downloadOk = $false
  try {
    Invoke-WebRequest -Uri $Url -OutFile $tmpPath -UseBasicParsing
    $downloadOk = $true
  } catch {
    Write-Warning "Invoke-WebRequest falló: $($_.Exception.Message). Intentando con curl.exe ..."
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
      try {
        & $curl.Source -L $Url -o $ProxyPath
        $downloadOk = Test-Path $ProxyPath
      } catch {
        Write-Warning "curl.exe falló: $($_.Exception.Message). Intentando con BITS ..."
      }
    }
    if (-not $downloadOk) {
      try {
        Start-BitsTransfer -Source $Url -Destination $tmpPath -ErrorAction Stop
        $downloadOk = $true
      } catch {
        Write-Error "No se pudo descargar el proxy. Descárgalo manualmente: $Url y guárdalo en $ProxyPath"
      }
    }
  }

  if ($downloadOk -and (Test-Path $tmpPath)) {
    Move-Item -Force $tmpPath $ProxyPath
  }
}

if (Test-Path $ProxyPath) {
  try { Unblock-File $ProxyPath } catch {}
} else {
  Write-Error "El ejecutable no existe en $ProxyPath. Aborta."
  return
}

Write-Host "Iniciando Cloud SQL Auth Proxy para $ConnectionName en puerto $Port ..."
Write-Host "Deja esta ventana abierta para mantener el proxy activo."

$args = @($ConnectionName, "--port", $Port)
if ($CredentialsFile -and (Test-Path $CredentialsFile)) {
  Write-Host "Usando credenciales: $CredentialsFile"
  $args += @("--credentials-file", $CredentialsFile)
} else {
  Write-Host "Usando credenciales por defecto (gcloud auth application-default)."
  Write-Host "Si falla la autenticación, ejecuta: gcloud auth login ; gcloud auth application-default login"
}

& $ProxyPath @args
