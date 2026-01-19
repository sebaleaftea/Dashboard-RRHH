# 🚀 Guía Completa: Desplegar a GCP sin instalar nada en tu PC

## 📋 Resumen

Vamos a desplegar **microservice-user** usando únicamente el navegador web, sin instalar gcloud CLI.

**Tiempo estimado:** 15-20 minutos

---

## Parte 1: Crear el Secret (Contraseña de DB)

### Paso 1.1: Ir a Secret Manager

1. Abre: https://console.cloud.google.com/security/secret-manager?project=prd-sfh-it-bi-erbi

2. Si ves un botón "ENABLE" o "HABILITAR", haz click (es para habilitar la API por primera vez)

### Paso 1.2: Crear Secret

1. Click en **"CREATE SECRET"** (botón azul arriba)

2. Completa el formulario:
   - **Name:** `grh-db-password`
   - **Secret value:** `c_}2ysUR"6dXEk]o`
   - **Replication:** Deja "Automatic"

3. Click **"CREATE"**

### Paso 1.3: Dar Permisos

1. En la lista de secrets, click en **grh-db-password**

2. Click en la pestaña **"PERMISSIONS"**

3. Click **"GRANT ACCESS"**

4. En **"New principals"** escribe:
   ```
   prd-sfh-it-bi-erbi@appspot.gserviceaccount.com
   ```

5. En **"Select a role"** busca y selecciona:
   ```
   Secret Manager Secret Accessor
   ```

6. Click **"SAVE"**

✅ **Secret creado correctamente!**

---

## Parte 2: Desplegar microservice-user usando Cloud Shell

### Paso 2.1: Abrir Cloud Shell

1. Ve a: https://console.cloud.google.com/?project=prd-sfh-it-bi-erbi

2. En la parte superior derecha, click en el ícono de terminal: **>_** (Activate Cloud Shell)

3. Se abrirá una terminal en la parte inferior del navegador

### Paso 2.2: Subir archivos del proyecto

**Opción A: Desde Git (Recomendado)**

Si tu código está en GitHub:

```bash
# Clonar repositorio
git clone https://github.com/sebaleaftea/Dashboard-RRHH.git
cd Dashboard-RRHH
```

**Opción B: Subir archivos manualmente**

1. En Cloud Shell, click en el ícono de **tres puntos** (⋮) arriba a la derecha
2. Selecciona **"Upload"**
3. Selecciona la carpeta completa de tu proyecto (comprimida en .zip)
4. Espera a que se suba
5. Descomprime: `unzip Dashboard-RRHH.zip`

### Paso 2.3: Ejecutar script de despliegue

```bash
# Dar permisos de ejecución al script
chmod +x scripts/deploy-user-cloudshell.sh

# Ejecutar despliegue
./scripts/deploy-user-cloudshell.sh
```

**El script hará automáticamente:**
1. ✅ Configurar proyecto GCP
2. ✅ Compilar código Java con Maven
3. ✅ Construir imagen Docker
4. ✅ Subir imagen a Container Registry
5. ✅ Desplegar en Cloud Run
6. ✅ Configurar conexión a Cloud SQL
7. ✅ Probar el servicio

**Tiempo:** ~10 minutos

### Paso 2.4: Ver resultado

Al finalizar verás algo como:

```
========================================
  DESPLIEGUE COMPLETADO
========================================

URL del servicio:
  https://microservice-user-xxxxx-uc.a.run.app

Endpoints disponibles:
  - GET  https://microservice-user-xxxxx-uc.a.run.app/api/user/all
  - POST https://microservice-user-xxxxx-uc.a.run.app/api/user/login
```

**¡Copia esta URL!** La necesitarás para el frontend.

---

## Parte 3: Verificar que funciona

### En Cloud Shell, prueba los endpoints:

```bash
# Guardar URL en variable (reemplaza con tu URL real)
SERVICE_URL="https://microservice-user-xxxxx-uc.a.run.app"

# Listar usuarios
curl $SERVICE_URL/api/user/all

# Probar login
curl -X POST $SERVICE_URL/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## Parte 4: Ver Logs y Monitoreo

### Ver logs en tiempo real:

```bash
gcloud run services logs read microservice-user \
  --region=southamerica-west1 \
  --tail
```

### O desde la consola web:

1. Ve a: https://console.cloud.google.com/run?project=prd-sfh-it-bi-erbi
2. Click en **microservice-user**
3. Click en la pestaña **"LOGS"**

---

## 🎯 Resumen de URLs importantes

Guarda estos enlaces:

| Recurso | URL |
|---------|-----|
| **Secret Manager** | https://console.cloud.google.com/security/secret-manager?project=prd-sfh-it-bi-erbi |
| **Cloud Run Services** | https://console.cloud.google.com/run?project=prd-sfh-it-bi-erbi |
| **Cloud Build History** | https://console.cloud.google.com/cloud-build/builds?project=prd-sfh-it-bi-erbi |
| **Cloud SQL** | https://console.cloud.google.com/sql/instances?project=prd-sfh-it-bi-erbi |
| **Logs** | https://console.cloud.google.com/logs?project=prd-sfh-it-bi-erbi |

---

## 🔧 Comandos útiles en Cloud Shell

```bash
# Ver servicios desplegados
gcloud run services list --region=southamerica-west1

# Ver detalles del servicio
gcloud run services describe microservice-user \
  --region=southamerica-west1

# Actualizar variables de entorno
gcloud run services update microservice-user \
  --region=southamerica-west1 \
  --set-env-vars="NEW_VAR=value"

# Ver revisiones (versiones)
gcloud run revisions list \
  --service=microservice-user \
  --region=southamerica-west1

# Eliminar servicio (si quieres empezar de nuevo)
gcloud run services delete microservice-user \
  --region=southamerica-west1
```

---

## 🚨 Troubleshooting

### Error: "Permission denied"

**Solución:** Verifica que tienes los roles necesarios:
- Cloud Run Admin
- Cloud Build Editor
- Service Account User

Pide a tu administrador GCP que te los asigne.

### Error: "Secret not found"

**Solución:** Verifica que creaste el secret `grh-db-password` correctamente en el Paso 1.

### Error: "Cloud SQL connection failed"

**Solución:** 
1. Verifica que Cloud SQL esté corriendo
2. Ve a: https://console.cloud.google.com/sql/instances
3. Verifica que `bdd-grh` esté en estado "Available"

### Servicio no responde

**Ver logs:**
```bash
gcloud run services logs read microservice-user \
  --region=southamerica-west1 \
  --limit=50
```

Busca líneas con "ERROR" o "Exception"

---

## 📝 Notas Importantes

1. **Cloud Shell se cierra después de inactividad**
   - Tus archivos se guardan en `/home/tu_usuario`
   - Puedes cerrar y volver más tarde

2. **El comando proxy NO es necesario en Cloud Run**
   - Cloud Run se conecta directamente a Cloud SQL
   - Es más rápido y seguro

3. **HTTPS automático**
   - Cloud Run te da HTTPS gratis
   - No necesitas configurar certificados

4. **Escalado automático**
   - Si no hay tráfico, escala a 0 (no pagas)
   - Si hay mucho tráfico, escala automáticamente hasta 10 instancias

---

## ✅ Checklist

- [ ] Secret `grh-db-password` creado
- [ ] Permisos del secret configurados
- [ ] Código subido a Cloud Shell
- [ ] Script de despliegue ejecutado
- [ ] URL del servicio copiada
- [ ] Endpoints probados y funcionando
- [ ] Logs revisados (sin errores)

---

## 🎯 Siguiente Paso

Una vez que microservice-user esté desplegado:

1. **Desplegar microservice-employee** (mismo proceso)
2. **Actualizar frontend** para usar las URLs de Cloud Run
3. **Desplegar frontend** en Cloud Run también

---

## 💡 Tip: Mantener Cloud Shell activo

Si el proceso es muy largo y no quieres que se cierre Cloud Shell:

```bash
# Ejecutar en background
nohup ./scripts/deploy-user-cloudshell.sh > deploy.log 2>&1 &

# Ver progreso
tail -f deploy.log
```

---

¿Listo para empezar? Comienza con el **Paso 1: Crear el Secret** 🚀
