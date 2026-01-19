#!/bin/bash
# Script de despliegue del frontend (React/Vite) en Cloud Run desde Google Cloud Shell

set -e

PROJECT_ID="prd-sfh-it-bi-erbi"
REGION="southamerica-west1"
SERVICE_NAME="dashboard-rrhh-front"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

cd Front/dashboard-rrhh

echo "1. Instalando dependencias y construyendo el frontend..."
npm install
npm run build

echo "2. Construyendo imagen Docker..."
gcloud builds submit --tag $IMAGE_NAME .

echo "3. Desplegando a Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image=$IMAGE_NAME \
  --platform=managed \
  --region=$REGION \
  --allow-unauthenticated \
  --port=80 \
  --memory=256Mi \
  --cpu=1 \
  --timeout=300 \
  --max-instances=3 \
  --min-instances=0

SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region=$REGION --format='value(status.url)')

echo "\n========================================"
echo "  FRONTEND DESPLEGADO EN CLOUD RUN"
echo "========================================"
echo "URL del frontend: $SERVICE_URL"
echo "========================================\n"
