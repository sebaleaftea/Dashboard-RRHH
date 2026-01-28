#!/bin/bash
# Script de despliegue de microservice-user para Google Cloud Shell
# Ejecutar desde: https://console.cloud.google.com/?cloudshell=true

set -e  # Salir si hay error

echo "========================================"
echo "  Despliegue microservice-user"
echo "  Desde Google Cloud Shell"
echo "========================================"
echo ""

# Variables
PROJECT_ID="prd-sfh-it-bi-erbi"
REGION="southamerica-west1"
SERVICE_NAME="microservice-user"
CLOUD_SQL_INSTANCE="prd-sfh-it-bi-erbi:southamerica-west1:gdh-massti"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "Proyecto: $PROJECT_ID"
echo "Región: $REGION"
echo "Servicio: $SERVICE_NAME"
echo ""

# Configurar proyecto
echo "1. Configurando proyecto..."
gcloud config set project $PROJECT_ID
echo "   OK"

# Verificar directorio
echo ""
echo "2. Verificando directorio..."
if [ ! -d "Backend/microservice-user" ]; then
    echo "   ERROR: No se encuentra Backend/microservice-user"
    echo "   Ejecuta primero:"
    echo "   git clone https://github.com/sebaleaftea/Dashboard-RRHH.git"
    echo "   cd Dashboard-RRHH"
    exit 1
fi
echo "   OK - Directorio encontrado"

# Verificar archivos necesarios
echo ""
echo "3. Verificando archivos necesarios..."
if [ ! -f "Backend/microservice-user/Dockerfile" ]; then
    echo "   ERROR: Dockerfile no encontrado"
    exit 1
fi
if [ ! -f "Backend/microservice-user/pom.xml" ]; then
    echo "   ERROR: pom.xml no encontrado"
    exit 1
fi
echo "   OK - Archivos encontrados"

# Compilar con Maven
echo ""
echo "4. Compilando con Maven..."
echo "   (Esto puede tomar varios minutos...)"
cd Backend/microservice-user
./mvnw clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "   ERROR: Compilación falló"
    exit 1
fi
echo "   OK - Compilación exitosa"

# Build y push imagen Docker
echo ""
echo "5. Construyendo imagen Docker..."
echo "   (Esto puede tomar varios minutos...)"
gcloud builds submit --tag $IMAGE_NAME
if [ $? -ne 0 ]; then
    echo "   ERROR: Build de imagen falló"
    exit 1
fi
echo "   OK - Imagen construida: $IMAGE_NAME"

# Deploy a Cloud Run
echo ""
echo "6. Desplegando a Cloud Run..."
echo "   (Configurando servicio...)"

gcloud run deploy $SERVICE_NAME \
  --image=$IMAGE_NAME \
  --platform=managed \
  --region=$REGION \
  --allow-unauthenticated \
  --port=8080 \
  --memory=512Mi \
  --cpu=1 \
  --timeout=300 \
  --max-instances=10 \
  --min-instances=0 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun,DB_NAME=grh,DB_USERNAME=grh-bdduser,CLOUD_SQL_INSTANCE=$CLOUD_SQL_INSTANCE" \
  --set-secrets="DB_PASSWORD=grh-db-password:latest" \
  --add-cloudsql-instances=$CLOUD_SQL_INSTANCE

if [ $? -ne 0 ]; then
    echo "   ERROR: Despliegue falló"
    exit 1
fi

# Obtener URL del servicio
echo ""
echo "7. Obteniendo URL del servicio..."
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region=$REGION --format="value(status.url)")

echo ""
echo "========================================"
echo "  DESPLIEGUE COMPLETADO"
echo "========================================"
echo ""
echo "URL del servicio:"
echo "  $SERVICE_URL"
echo ""
echo "Endpoints disponibles:"
echo "  - GET  $SERVICE_URL/api/user/all"
echo "  - POST $SERVICE_URL/api/user/login"
echo "  - POST $SERVICE_URL/api/user/create"
echo ""
echo "Para ver logs en tiempo real:"
echo "  gcloud run services logs read $SERVICE_NAME --region=$REGION --tail"
echo ""
echo "Para ver detalles del servicio:"
echo "  gcloud run services describe $SERVICE_NAME --region=$REGION"
echo ""

# Probar servicio
echo "8. Probando servicio desplegado..."
sleep 5

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$SERVICE_URL/api/user/all")
if [ "$HTTP_CODE" = "200" ]; then
    echo "   OK - Servicio responde correctamente (HTTP $HTTP_CODE)"
    echo ""
    echo "Probando listar usuarios:"
    curl -s "$SERVICE_URL/api/user/all" | head -c 500
    echo ""
else
    echo "   ADVERTENCIA: Servicio respondió con HTTP $HTTP_CODE"
    echo "   Verifica los logs para diagnosticar"
fi

echo ""
echo "========================================"
echo "  Siguiente paso:"
echo "  Actualiza el frontend para usar esta URL"
echo "========================================"
echo ""
