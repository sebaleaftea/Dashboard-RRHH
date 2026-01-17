#!/bin/bash
# Script de Despliegue ETL para Cloud Shell
# Ejecutar en: https://console.cloud.google.com/?cloudshell=true

set -e

PROJECT_ID="prd-sfh-it-bi-erbi"
REGION="southamerica-west1"
INSTANCE_CONNECTION="prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh"
IMAGE_NAME="gcr.io/prd-sfh-it-bi-erbi/employee-etl:latest"
SERVICE_ACCOUNT="etl-runner"
JOB_NAME="employee-etl-job"

echo "=== Configurando proyecto ==="
gcloud config set project $PROJECT_ID

echo ""
echo "=== Paso 1: Crear Service Account ==="
gcloud iam service-accounts create $SERVICE_ACCOUNT \
  --display-name="ETL Runner for Talana" \
  --project=$PROJECT_ID || echo "Service account ya existe"

SA_EMAIL="${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com"

echo ""
echo "=== Paso 2: Asignar permisos IAM ==="
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudsql.client"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/logging.logWriter"

echo ""
echo "=== Paso 3: Verificar archivos ETL ==="
ls -la etl/ || echo "ERROR: Carpeta etl/ no encontrada. Sube los archivos primero."

echo ""
echo "=== Paso 4: Construir imagen Docker ==="
cd etl
gcloud builds submit --tag $IMAGE_NAME --project=$PROJECT_ID
cd ..

echo ""
echo "=== Paso 5: Eliminar job anterior si existe ==="
gcloud run jobs delete $JOB_NAME \
  --region=$REGION \
  --project=$PROJECT_ID \
  --quiet || echo "Job no existia"

echo ""
echo "=== Paso 6: Crear Cloud Run Job ==="
gcloud run jobs create $JOB_NAME \
  --image=$IMAGE_NAME \
  --region=$REGION \
  --task-timeout=1200s \
  --set-secrets=TALANA_TOKEN=talana-api-token:latest \
  --set-env-vars="TALANA_URL=https://talana.com/es/api/contrato-paginado/,INSTANCE_CONNECTION_NAME=${INSTANCE_CONNECTION},DB_NAME=grh,DB_USER=grh-bdduser,DB_PASS=c_}2ysUR\"6dXEk]o" \
  --add-cloudsql-instances=$INSTANCE_CONNECTION \
  --service-account=$SA_EMAIL \
  --memory=1Gi \
  --cpu=1 \
  --max-retries=0 \
  --project=$PROJECT_ID

echo ""
echo "=== Paso 7: Ejecutar job ==="
gcloud run jobs execute $JOB_NAME \
  --region=$REGION \
  --project=$PROJECT_ID \
  --wait

echo ""
echo "=== Paso 8: Ver logs ==="
gcloud run jobs executions list \
  --job=$JOB_NAME \
  --region=$REGION \
  --project=$PROJECT_ID \
  --limit=5

echo ""
echo "=============================================="
echo "✓ Despliegue completado!"
echo ""
echo "Ver logs completos:"
echo "  gcloud logging read \"resource.type=cloud_run_job AND resource.labels.job_name=${JOB_NAME}\" --project=${PROJECT_ID} --limit=200"
echo ""
echo "Verificar datos en Cloud SQL:"
echo "  gcloud sql connect bdd-grh --user=grh-bdduser --database=grh --project=${PROJECT_ID}"
echo "  Password: c_}2ysUR\"6dXEk]o"
echo ""
echo "Consultas SQL para verificar:"
echo "  SELECT COUNT(*) FROM empleado;"
echo "  SELECT COUNT(*) FROM contrato;"
echo "  SELECT COUNT(*) FROM centro_costo;"
echo "  SELECT COUNT(*) FROM sucursal;"
echo "=============================================="
