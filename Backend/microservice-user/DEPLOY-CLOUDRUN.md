# Guía Rápida: Desplegar microservice-user a Cloud Run

## ✅ Pre-requisitos

1. **gcloud CLI instalado**
   ```powershell
   # Verificar instalación
   gcloud version
   
   # Si no está instalado, descarga desde:
   # https://cloud.google.com/sdk/docs/install
   ```

2. **Autenticación en GCP**
   ```powershell
   # Login
   gcloud auth login
   
   # Configurar proyecto
   gcloud config set project prd-sfh-it-bi-erbi
   ```

3. **Docker Desktop instalado y corriendo**
   ```powershell
   # Verificar Docker
   docker --version
   ```

4. **Habilitar APIs necesarias**
   ```powershell
   gcloud services enable run.googleapis.com
   gcloud services enable cloudbuild.googleapis.com
   gcloud services enable secretmanager.googleapis.com
   gcloud services enable sqladmin.googleapis.com
   ```

---

## 🚀 Pasos de Despliegue

### Paso 1: Configurar Secrets (solo primera vez)

```powershell
cd "c:\Workspace\Dashboard RRHH\scripts"
.\setup-gcp-secrets.ps1
```

Este script:
- ✅ Crea el secret `grh-db-password` con la contraseña de Cloud SQL
- ✅ Configura permisos para que Cloud Run pueda accederlo

### Paso 2: Desplegar a Cloud Run

```powershell
cd "c:\Workspace\Dashboard RRHH\scripts"
.\deploy-user-cloudrun.ps1
```

Este script:
1. ✅ Compila el proyecto con Maven
2. ✅ Construye la imagen Docker
3. ✅ Sube la imagen a Container Registry
4. ✅ Despliega en Cloud Run
5. ✅ Configura conexión a Cloud SQL
6. ✅ Prueba el servicio

**Tiempo estimado:** 5-10 minutos

---

## 🧪 Verificar Despliegue

### 1. Obtener URL del servicio

```powershell
gcloud run services describe microservice-user --region=southamerica-west1 --format="value(status.url)"
```

### 2. Probar endpoints

```powershell
# Obtener URL
$serviceUrl = gcloud run services describe microservice-user --region=southamerica-west1 --format="value(status.url)"

# Listar usuarios
Invoke-WebRequest -Uri "$serviceUrl/api/user/all" -UseBasicParsing

# Probar login
$body = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

Invoke-WebRequest -Uri "$serviceUrl/api/user/login" `
    -Method POST `
    -Body $body `
    -ContentType "application/json" `
    -UseBasicParsing
```

### 3. Ver logs en tiempo real

```powershell
gcloud run services logs read microservice-user --region=southamerica-west1 --tail
```

### 4. Ver detalles del servicio

```powershell
gcloud run services describe microservice-user --region=southamerica-west1
```

---

## 🔧 Comandos Útiles

### Actualizar variables de entorno

```powershell
gcloud run services update microservice-user `
  --region=southamerica-west1 `
  --set-env-vars="NEW_VAR=value"
```

### Actualizar solo la imagen (sin recompilar)

```powershell
gcloud run deploy microservice-user `
  --image=gcr.io/prd-sfh-it-bi-erbi/microservice-user `
  --region=southamerica-west1
```

### Eliminar servicio

```powershell
gcloud run services delete microservice-user --region=southamerica-west1
```

### Ver todas las revisiones

```powershell
gcloud run revisions list --service=microservice-user --region=southamerica-west1
```

### Revertir a revisión anterior

```powershell
gcloud run services update-traffic microservice-user `
  --region=southamerica-west1 `
  --to-revisions=REVISION_NAME=100
```

---

## 📊 Configuración Actual

| Parámetro | Valor |
|-----------|-------|
| **Proyecto** | prd-sfh-it-bi-erbi |
| **Región** | southamerica-west1 |
| **Servicio** | microservice-user |
| **Puerto** | 8080 |
| **Memoria** | 512Mi |
| **CPU** | 1 |
| **Min Instances** | 0 (escala a 0) |
| **Max Instances** | 10 |
| **Timeout** | 300s |
| **Cloud SQL** | prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh |

---

## 🔒 Seguridad

- ✅ Contraseñas almacenadas en Secret Manager
- ✅ Conexión directa a Cloud SQL (sin IPs públicas)
- ✅ HTTPS automático
- ✅ Usuario no-root en contenedor
- ✅ Service Account con permisos mínimos

---

## 💰 Costos Estimados

**Capa Gratuita de Cloud Run:**
- 2 millones de requests/mes
- 360,000 GB-seconds de compute
- 180,000 vCPU-seconds

**Con poco tráfico:** ~$0-5 USD/mes

---

## 🐛 Troubleshooting

### Error: "Permission denied"
```powershell
# Verificar permisos
gcloud projects get-iam-policy prd-sfh-it-bi-erbi

# Agregar rol necesario
gcloud projects add-iam-policy-binding prd-sfh-it-bi-erbi `
  --member="user:TU_EMAIL@mass.cl" `
  --role="roles/run.admin"
```

### Error: "Cloud SQL connection failed"
```powershell
# Verificar que Cloud SQL esté corriendo
gcloud sql instances describe bdd-grh

# Verificar conexión
gcloud sql instances describe bdd-grh --format="value(connectionName)"
```

### Error: "Secret not found"
```powershell
# Crear secret manualmente
.\setup-gcp-secrets.ps1
```

### Servicio no responde
```powershell
# Ver logs para diagnosticar
gcloud run services logs read microservice-user `
  --region=southamerica-west1 `
  --limit=50
```

---

## 📝 Notas Importantes

1. **NO necesitas Cloud SQL Proxy en producción** - Cloud Run se conecta directamente
2. **El puerto debe ser 8080** - Cloud Run lo requiere
3. **Las variables de entorno se pasan en el deploy** - No las incluyas en el código
4. **Los secrets nunca se exponen** - Se inyectan de forma segura en runtime

---

## 🎯 Próximos Pasos

Después de desplegar microservice-user exitosamente:

1. ✅ Desplegar microservice-employee de la misma manera
2. ✅ Desplegar frontend React
3. ✅ Configurar dominio custom (opcional)
4. ✅ Configurar CI/CD con Cloud Build
5. ✅ Configurar monitoreo y alertas

---

## 🆘 Ayuda

Si tienes problemas:
1. Revisa los logs: `gcloud run services logs read microservice-user --region=southamerica-west1`
2. Verifica el estado: `gcloud run services describe microservice-user --region=southamerica-west1`
3. Consulta la documentación: https://cloud.google.com/run/docs
