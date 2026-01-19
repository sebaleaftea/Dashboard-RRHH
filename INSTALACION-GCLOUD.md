# Guía de Instalación - Google Cloud SDK (gcloud CLI)

## 📥 Instalación en Windows

### Opción 1: Instalador Directo (Recomendado)

1. **Descargar instalador:**
   - Ve a: https://cloud.google.com/sdk/docs/install
   - Descarga: `GoogleCloudSDKInstaller.exe`

2. **Ejecutar instalador:**
   - Doble clic en el archivo descargado
   - Sigue el asistente de instalación
   - Marca la opción: "Add gcloud to PATH"
   - Marca la opción: "Run 'gcloud init'"

3. **Verificar instalación:**
   ```powershell
   # Reinicia PowerShell
   gcloud version
   ```

### Opción 2: PowerShell (Alternativa)

```powershell
# Descargar instalador
$url = "https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe"
$output = "$env:TEMP\GoogleCloudSDKInstaller.exe"
Invoke-WebRequest -Uri $url -OutFile $output

# Ejecutar instalador
Start-Process -FilePath $output -Wait

# Agregar al PATH (si no se agregó automáticamente)
$gcloudPath = "$env:LOCALAPPDATA\Google\Cloud SDK\google-cloud-sdk\bin"
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";$gcloudPath", [EnvironmentVariableTarget]::User)

# Reiniciar PowerShell
```

---

## 🔐 Configuración Inicial

### 1. Autenticación

```powershell
# Login con tu cuenta de Google
gcloud auth login
```

Se abrirá un navegador para que te autentiques.

### 2. Configurar Application Default Credentials

```powershell
# Esto permite que las aplicaciones locales se conecten a GCP
gcloud auth application-default login
```

### 3. Configurar Proyecto

```powershell
# Configurar proyecto por defecto
gcloud config set project prd-sfh-it-bi-erbi

# Verificar configuración
gcloud config list
```

### 4. Verificar Acceso

```powershell
# Listar proyectos disponibles
gcloud projects list

# Verificar acceso a Cloud SQL
gcloud sql instances list

# Verificar región por defecto
gcloud config set compute/region southamerica-west1
```

---

## ✅ Verificación Post-Instalación

```powershell
# Verificar versión
gcloud version

# Verificar componentes instalados
gcloud components list

# Actualizar componentes (opcional)
gcloud components update
```

**Salida esperada:**
```
Google Cloud SDK 458.0.0
bq 2.0.101
core 2024.01.12
gcloud-crc32c 1.0.0
gsutil 5.27
```

---

## 🔧 Habilitar APIs Necesarias

```powershell
# Habilitar APIs requeridas para el proyecto
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable sqladmin.googleapis.com
gcloud services enable containerregistry.googleapis.com
```

---

## 🚨 Troubleshooting

### "gcloud no se reconoce"

**Causa:** No está en el PATH

**Solución:**
```powershell
# Encontrar instalación de gcloud
Get-ChildItem -Path "C:\" -Filter "gcloud.cmd" -Recurse -ErrorAction SilentlyContinue

# Agregar al PATH manualmente
$gcloudPath = "C:\Users\TU_USUARIO\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin"
$env:Path += ";$gcloudPath"

# Hacer permanente
[Environment]::SetEnvironmentVariable("Path", $env:Path, [EnvironmentVariableTarget]::User)
```

### "Authentication failed"

```powershell
# Limpiar credenciales y volver a autenticar
gcloud auth revoke
gcloud auth login
gcloud auth application-default login
```

### "Permission denied"

**Verifica que tu cuenta tenga los roles necesarios:**
- Cloud Run Admin
- Cloud SQL Admin
- Secret Manager Admin
- Service Account User

Pide a tu administrador de GCP que te asigne estos roles.

---

## 📋 Comandos Útiles Post-Instalación

```powershell
# Ver información de la cuenta
gcloud auth list

# Ver configuración actual
gcloud config configurations list

# Cambiar entre configuraciones
gcloud config configurations activate NOMBRE

# Ver cuota de recursos
gcloud compute project-info describe --project=prd-sfh-it-bi-erbi

# Ver instancias de Cloud SQL
gcloud sql instances list --project=prd-sfh-it-bi-erbi

# Ver servicios de Cloud Run
gcloud run services list --region=southamerica-west1
```

---

## 🎯 Siguiente Paso

Una vez instalado y configurado, ejecuta:

```powershell
cd "c:\Workspace\Dashboard RRHH\scripts"
.\setup-gcp-secrets.ps1
```

---

## 💡 Alternativa: Usar Google Cloud Console

Si no puedes instalar gcloud CLI, puedes crear los secrets manualmente desde la consola web:

1. Ve a: https://console.cloud.google.com/security/secret-manager
2. Selecciona proyecto: `prd-sfh-it-bi-erbi`
3. Click en "CREATE SECRET"
4. Name: `grh-db-password`
5. Secret value: `c_}2ysUR"6dXEk]o`
6. Click en "CREATE"

Luego configura permisos:
1. Click en el secret creado
2. Tab "PERMISSIONS"
3. Click "GRANT ACCESS"
4. Principal: `PROJECT_NUMBER-compute@developer.gserviceaccount.com`
5. Role: "Secret Manager Secret Accessor"
6. Click "SAVE"

---

## 📞 Soporte

- Documentación oficial: https://cloud.google.com/sdk/docs
- Troubleshooting: https://cloud.google.com/sdk/docs/troubleshooting
- Stack Overflow: https://stackoverflow.com/questions/tagged/google-cloud-sdk
