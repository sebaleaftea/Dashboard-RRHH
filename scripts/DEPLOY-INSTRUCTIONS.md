# 🚀 Guía de Despliegue ETL - Cloud Console

## Configuración Detectada
- **URL Talana**: https://talana.com/es/api/contrato-paginado/
- **Token Secret**: talana-api-token (ya guardado en Secret Manager)
- **Cloud SQL**: bdd-grh (prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh)
- **Base de datos**: grh
- **Usuario**: grh-bdduser

---

## Paso 1: Crear Service Account

1. Ve a: https://console.cloud.google.com/iam-admin/serviceaccounts?project=prd-sfh-it-bi-erbi

2. Click en **"CREAR CUENTA DE SERVICIO"**

3. Completa:
   - **Nombre**: `etl-runner`
   - **Descripción**: `Service Account para ETL de Talana`
   - Click **"CREAR Y CONTINUAR"**

4. Asignar roles (agregar 3 roles):
   - `Cloud SQL Client`
   - `Secret Manager Secret Accessor`
   - `Logs Writer`
   - Click **"CONTINUAR"** y **"LISTO"**

---

## Paso 2: Crear Secretos para Credenciales DB (Opcional)

Si prefieres usar Secret Manager para las credenciales de la base de datos:

1. Ve a: https://console.cloud.google.com/security/secret-manager?project=prd-sfh-it-bi-erbi

2. Crear `db-user`:
   - Click **"CREAR SECRETO"**
   - Nombre: `db-user`
   - Valor: `grh-bdduser`
   - Click **"CREAR SECRETO"**

3. Crear `db-pass`:
   - Click **"CREAR SECRETO"**
   - Nombre: `db-pass`
   - Valor: `c_}2ysUR"6dXEk]o`
   - Click **"CREAR SECRETO"**

---

## Paso 3: Construir Imagen Docker con Cloud Build

### Opción A: Usar Cloud Build (Recomendado)

1. Abre Cloud Shell: https://console.cloud.google.com/?cloudshell=true

2. Clona tu repositorio o sube los archivos ETL:
   ```bash
   # Si tienes el repo en GitHub
   git clone https://github.com/sebaleaftea/Dashboard-RRHH.git
   cd Dashboard-RRHH
   
   # O crea la estructura manualmente
   mkdir -p etl
   # Sube los archivos: etl.py, Dockerfile, requirements.txt
   ```

3. Construye la imagen:
   ```bash
   cd etl
   gcloud builds submit --tag gcr.io/prd-sfh-it-bi-erbi/employee-etl:latest --project=prd-sfh-it-bi-erbi
   ```

### Opción B: Subir archivos directamente a Cloud Shell

1. Abre Cloud Shell: https://console.cloud.google.com/?cloudshell=true

2. Click en el menú ⋮ (arriba derecha) → **"Subir"**

3. Sube estos 3 archivos:
   - `etl.py`
   - `Dockerfile`
   - `requirements.txt`

4. Crea el directorio y mueve los archivos:
   ```bash
   mkdir -p etl
   mv etl.py Dockerfile requirements.txt etl/
   cd etl
   ```

5. Construye la imagen:
   ```bash
   gcloud builds submit --tag gcr.io/prd-sfh-it-bi-erbi/employee-etl:latest --project=prd-sfh-it-bi-erbi
   ```

---

## Paso 4: Crear Cloud Run Job

1. Ve a: https://console.cloud.google.com/run/jobs?project=prd-sfh-it-bi-erbi

2. Click **"CREAR JOB"**

3. Configuración del contenedor:
   - **Imagen de contenedor**: `gcr.io/prd-sfh-it-bi-erbi/employee-etl:latest`
   - Click **"SELECCIONAR"** y busca la imagen creada

4. Configuración del job:
   - **Nombre del job**: `employee-etl-job`
   - **Región**: `southamerica-west1`
   - **Número de tareas**: `1`
   - **Paralelismo**: `1`

5. Configuración de contenedor → **Variables y secretos**:
   
   **Variables de entorno**:
   - `TALANA_URL` = `https://talana.com/es/api/contrato-paginado/`
   - `INSTANCE_CONNECTION_NAME` = `prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh`
   - `DB_NAME` = `grh`
   - `DB_USER` = `grh-bdduser`
   - `DB_PASS` = `c_}2ysUR"6dXEk]o`
   
   **Referencias a secretos**:
   - Variable: `TALANA_TOKEN`
   - Secreto: `talana-api-token`
   - Versión: `latest`

6. Configuración → **Conexiones**:
   - Marcar: ✅ **"Conectar a Cloud SQL"**
   - Seleccionar: `prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh`

7. Configuración → **Seguridad**:
   - **Cuenta de servicio**: Seleccionar `etl-runner@prd-sfh-it-bi-erbi.iam.gserviceaccount.com`

8. Configuración → **Capacidad**:
   - **Memoria**: `1 GiB`
   - **CPU**: `1`
   - **Tiempo de espera de ejecución de tareas**: `1200` segundos (20 minutos)

9. Click **"CREAR"**

---

## Paso 5: Ejecutar el Job Manualmente

1. En la página del job, click **"EJECUTAR"**

2. Monitorear los logs:
   - Click en la ejecución más reciente
   - Revisar logs en tiempo real

3. Verificar datos en Cloud SQL:
   ```sql
   SELECT COUNT(*) FROM empleado;
   SELECT COUNT(*) FROM contrato;
   SELECT COUNT(*) FROM centro_costo;
   SELECT COUNT(*) FROM sucursal;
   ```

---

## Paso 6: Programar Ejecución Periódica con Cloud Scheduler

1. Ve a: https://console.cloud.google.com/cloudscheduler?project=prd-sfh-it-bi-erbi

2. Click **"CREAR JOB"**

3. Configuración:
   - **Nombre**: `employee-etl-schedule`
   - **Región**: `southamerica-west1`
   - **Frecuencia**: `0 */6 * * *` (cada 6 horas)
   - **Zona horaria**: `America/Santiago`

4. Configurar la ejecución:
   - **Tipo de destino**: `HTTP`
   - **URL**: `https://southamerica-west1-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/prd-sfh-it-bi-erbi/jobs/employee-etl-job:run`
   - **Método HTTP**: `POST`

5. Autenticación:
   - **Auth header**: `Agregar encabezado de autenticación OIDC`
   - **Cuenta de servicio**: `etl-runner@prd-sfh-it-bi-erbi.iam.gserviceaccount.com`

6. Click **"CREAR"**

---

## ✅ Verificación Final

### Verificar en Cloud SQL Console:

```sql
-- Ver tablas creadas
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Contar registros
SELECT 
  (SELECT COUNT(*) FROM empleado) as empleados,
  (SELECT COUNT(*) FROM contrato) as contratos,
  (SELECT COUNT(*) FROM centro_costo) as centros_costo,
  (SELECT COUNT(*) FROM sucursal) as sucursales;

-- Ver últimos contratos insertados
SELECT * FROM contrato ORDER BY id DESC LIMIT 10;

-- Ver empleados con sus contratos vigentes
SELECT e.rut, e.nombre, e.ap_paterno, c.cargo, s.nombre as sucursal
FROM empleado e
JOIN contrato c ON c.empleado_id = e.id
LEFT JOIN sucursal s ON s.id = c.sucursal_id
WHERE c.vigente = true
LIMIT 20;
```

---

## 🔍 Troubleshooting

### Si el job falla:

1. **Ver logs detallados**:
   - Ve a Cloud Run Jobs → employee-etl-job → Logs
   - Busca errores de conexión, autenticación o datos

2. **Errores comunes**:
   - **"relation does not exist"**: DDL no aplicado → volver a aplicar DDL
   - **"authentication failed"**: Verificar password en variables de entorno
   - **"permission denied"**: Service account sin permisos → revisar IAM roles
   - **"connection refused"**: Cloud SQL instance no conectada → revisar conexión

3. **Probar conexión manualmente**:
   ```bash
   # En Cloud Shell
   gcloud sql connect bdd-grh --user=grh-bdduser --database=grh --project=prd-sfh-it-bi-erbi
   # Contraseña: c_}2ysUR"6dXEk]o
   
   # Verificar tablas
   \dt
   
   # Ver datos
   SELECT COUNT(*) FROM empleado;
   ```

---

## 📝 Notas Importantes

1. **Token de Talana**: El token `8a0465a1a4ecf4c3c7a56154e7920839fcdffd42` está hardcodeado en `application.properties` pero también guardado en Secret Manager. El ETL usa el de Secret Manager.

2. **Rate Limiting**: Talana recomienda máx 20 requests/min. El ETL maneja esto con pausas y reintentos.

3. **Mapeo de Campos**: El script `process_contract()` en `etl.py` tiene un mapeo genérico. Si los campos de Talana son diferentes, ajusta las líneas 147-175 de `etl.py`.

4. **Primera Ejecución**: Puede tardar varios minutos dependiendo de la cantidad de contratos en Talana.

5. **Monitoreo**: Revisa los logs regularmente en Cloud Logging para detectar problemas.
