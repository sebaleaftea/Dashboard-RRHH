# Guía Visual: Crear Secret en Google Cloud Console

## 🔐 Paso 1: Acceder a Secret Manager

1. Abre tu navegador
2. Ve a: **https://console.cloud.google.com/security/secret-manager**
3. Si te pide seleccionar proyecto, elige: **prd-sfh-it-bi-erbi**

---

## 📝 Paso 2: Crear el Secret

1. Click en el botón **"CREATE SECRET"** (arriba, color azul)

2. En el formulario, completa:

   **Name:**
   ```
   grh-db-password
   ```

   **Secret value:**
   ```
   c_}2ysUR"6dXEk]o
   ```

   **Replication policy:** 
   - Deja seleccionado: **"Automatic"**

3. Click en **"CREATE"** (abajo)

---

## 🔑 Paso 3: Configurar Permisos

1. En la lista de secrets, click en el secret que acabas de crear: **grh-db-password**

2. Ve a la pestaña **"PERMISSIONS"** (arriba)

3. Click en **"GRANT ACCESS"** (botón azul)

4. En el campo **"New principals"**, ingresa:
   ```
   PROJECT_NUMBER-compute@developer.gserviceaccount.com
   ```
   
   **⚠️ IMPORTANTE:** Necesitas reemplazar `PROJECT_NUMBER` con el número de tu proyecto.
   
   **Para obtener el PROJECT_NUMBER:**
   - Opción A: Ve a https://console.cloud.google.com/home/dashboard
   - Arriba verás "Project info", el número está ahí como "Project number"
   
   O usa esta Service Account (también funciona):
   ```
   prd-sfh-it-bi-erbi@appspot.gserviceaccount.com
   ```

5. En **"Select a role"**, busca y selecciona:
   ```
   Secret Manager Secret Accessor
   ```

6. Click en **"SAVE"**

---

## ✅ Paso 4: Verificar

1. Ve de nuevo a la lista de secrets
2. Deberías ver **grh-db-password** en la lista
3. El ícono de candado debe estar cerrado (significa que está protegido)

---

## 🎯 Listo!

Una vez completados estos pasos, el secret estará disponible para Cloud Run.

**Siguiente paso:** Desplegar microservice-user a Cloud Run

Para esto, necesitarás usar Cloud Shell (terminal en el navegador de GCP) o instalar gcloud CLI.

---

## 🌐 Alternativa: Usar Cloud Shell

Si no quieres instalar gcloud CLI en tu PC, puedes usar **Cloud Shell** (terminal en el navegador):

1. Ve a: https://console.cloud.google.com
2. Click en el ícono de terminal (arriba a la derecha): **>_**
3. Se abrirá una terminal en el navegador
4. Ejecuta los comandos de despliegue desde ahí

**Ventaja:** No necesitas instalar nada en tu PC.

---

## 📋 Comandos para Cloud Shell

Una vez que tengas el secret creado, desde Cloud Shell ejecuta:

```bash
# 1. Clonar el repositorio (si no lo tienes)
git clone https://github.com/sebaleaftea/Dashboard-RRHH.git
cd Dashboard-RRHH

# 2. Ir al directorio del microservicio
cd Backend/microservice-user

# 3. Build con Maven
./mvnw clean package -DskipTests

# 4. Build y push imagen Docker
gcloud builds submit --tag gcr.io/prd-sfh-it-bi-erbi/microservice-user

# 5. Deploy a Cloud Run
gcloud run deploy microservice-user \
  --image=gcr.io/prd-sfh-it-bi-erbi/microservice-user \
  --platform=managed \
  --region=southamerica-west1 \
  --allow-unauthenticated \
  --port=8080 \
  --memory=512Mi \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun,DB_NAME=grh,DB_USERNAME=grh-bdduser,CLOUD_SQL_INSTANCE=prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh" \
  --set-secrets="DB_PASSWORD=grh-db-password:latest" \
  --add-cloudsql-instances=prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh
```

---

## 🤔 ¿Qué Prefieres?

1. **Cloud Shell** (terminal en el navegador) - No instalas nada
2. **Instalar gcloud CLI** en tu PC - Más control local

Para cualquiera de las dos opciones, primero crea el secret siguiendo los pasos de arriba.
