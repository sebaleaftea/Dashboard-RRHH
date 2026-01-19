# Guía de Despliegue a GCP - Dashboard RRHH

## 🎯 Objetivo
Desplegar Backend (Spring Boot) y Frontend (React) en Google Cloud Platform conectándose a Cloud SQL sin necesidad del proxy local.

---

## 📊 Arquitectura Propuesta

```
Internet → Cloud Load Balancer
                ↓
         Cloud Run (Frontend React)
                ↓
         Cloud Run (Backend Spring Boot)
                ↓
         Cloud SQL PostgreSQL (grh)
```

**Ventajas:**
- ✅ Sin necesidad de Cloud SQL Proxy
- ✅ Conexión directa y segura
- ✅ Escalado automático
- ✅ Pago por uso
- ✅ SSL/TLS automático

---

## 🏗️ Arquitectura de Servicios

### Backend Microservicios
1. **microservice-employee** (Puerto 8082) - Gestión de empleados
2. **microservice-user** (Puerto 8081) - Autenticación
3. **microservice-config** (Puerto 8888) - Configuración (opcional)
4. **microservice-eureka** (Puerto 8761) - Service Discovery (opcional)
5. **microservice-gateway** (Puerto 8080) - API Gateway (opcional)

### Frontend
- **React + Vite** - Dashboard RRHH

---

## 📦 Opción 1: Cloud Run (Recomendado - Más Simple)

### Ventajas
- Sin gestión de infraestructura
- Escalado automático a 0 (sin costo cuando no hay tráfico)
- Deploy en minutos
- HTTPS automático
- Ideal para aplicaciones stateless

### Pasos de Despliegue

#### 1. Preparar Backend (Spring Boot)

**A. Crear Dockerfile para cada microservicio**

```dockerfile
# Backend/microservice-employee/Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**B. Actualizar configuración para Cloud Run**

```properties
# application-cloudrun.properties
server.port=8080  # Cloud Run usa puerto 8080 por defecto

# Cloud SQL Connection usando Socket Unix (sin proxy)
spring.datasource.url=jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${CLOUD_SQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**C. Agregar dependencia Cloud SQL Socket Factory**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>postgres-socket-factory</artifactId>
    <version>1.19.0</version>
</dependency>
```

**D. Build y Deploy**

```bash
# Build JAR
cd Backend/microservice-employee
./mvnw clean package -DskipTests

# Build imagen Docker
gcloud builds submit --tag gcr.io/prd-sfh-it-bi-erbi/microservice-employee

# Deploy a Cloud Run
gcloud run deploy microservice-employee \
  --image gcr.io/prd-sfh-it-bi-erbi/microservice-employee \
  --platform managed \
  --region southamerica-west1 \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloudrun" \
  --set-env-vars="DB_NAME=grh" \
  --set-env-vars="DB_USERNAME=grh-bdduser" \
  --set-env-vars="CLOUD_SQL_INSTANCE=prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh" \
  --set-secrets="DB_PASSWORD=grh-db-password:latest" \
  --add-cloudsql-instances prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh
```

#### 2. Preparar Frontend (React)

**A. Crear Dockerfile optimizado**

```dockerfile
# Front/dashboard-rrhh/Dockerfile
# Stage 1: Build
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Production
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
```

**B. Crear nginx.conf**

```nginx
# Front/dashboard-rrhh/nginx.conf
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    server {
        listen 8080;
        root /usr/share/nginx/html;
        index index.html;

        location / {
            try_files $uri $uri/ /index.html;
        }

        # Caché para assets estáticos
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

**C. Actualizar variables de entorno en React**

```javascript
// src/config/api.js
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

export const API_ENDPOINTS = {
  USER: `${API_BASE_URL}/api/user`,
  EMPLOYEE: `${API_BASE_URL}/api/db/empleados`,
};
```

**D. Build y Deploy**

```bash
cd Front/dashboard-rrhh

# Build imagen Docker
gcloud builds submit --tag gcr.io/prd-sfh-it-bi-erbi/dashboard-rrhh-frontend

# Deploy a Cloud Run
gcloud run deploy dashboard-rrhh-frontend \
  --image gcr.io/prd-sfh-it-bi-erbi/dashboard-rrhh-frontend \
  --platform managed \
  --region southamerica-west1 \
  --allow-unauthenticated \
  --set-env-vars="VITE_API_URL=https://microservice-employee-xxxxx.run.app"
```

---

## 📦 Opción 2: Google Kubernetes Engine (GKE)

### Ventajas
- Control total sobre la infraestructura
- Orquestación avanzada
- Service mesh (Istio)
- Ideal para microservicios complejos

### Desventajas
- Más complejo de configurar
- Costo base más alto (cluster siempre corriendo)
- Requiere conocimientos de Kubernetes

**Recomendación:** Usa Cloud Run primero. Si creces, migra a GKE.

---

## 📦 Opción 3: App Engine

### Ventajas
- Plataforma tradicional PaaS
- Fácil de usar
- Escalado automático

### Desventajas
- Menos flexible que Cloud Run
- No soporta todos los runtimes
- Puede ser más costoso

---

## 🔐 Gestión de Secretos

**Usar Secret Manager (NO variables de entorno para passwords)**

```bash
# Crear secret para la contraseña de DB
echo -n 'c_}2ysUR"6dXEk]o' | gcloud secrets create grh-db-password \
  --data-file=- \
  --replication-policy="automatic"

# Dar permiso a Cloud Run para leer el secret
gcloud secrets add-iam-policy-binding grh-db-password \
  --member="serviceAccount:PROJECT_NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

---

## 🔄 CI/CD Automatizado

### Cloud Build + GitHub

**cloudbuild.yaml (Backend)**

```yaml
steps:
  # Build JAR
  - name: 'maven:3.8-openjdk-17'
    entrypoint: 'mvn'
    args: ['clean', 'package', '-DskipTests']
    dir: 'Backend/microservice-employee'

  # Build Docker image
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/$PROJECT_ID/microservice-employee', 'Backend/microservice-employee']

  # Push to Container Registry
  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/microservice-employee']

  # Deploy to Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: gcloud
    args:
      - 'run'
      - 'deploy'
      - 'microservice-employee'
      - '--image=gcr.io/$PROJECT_ID/microservice-employee'
      - '--region=southamerica-west1'
      - '--platform=managed'
      - '--allow-unauthenticated'
      - '--add-cloudsql-instances=prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh'

images:
  - 'gcr.io/$PROJECT_ID/microservice-employee'
```

---

## 🧪 Plan de Pruebas

### 1. Desarrollo Local (Actual)
```
Frontend → localhost:5173
Backend → localhost:8081, 8082
Database → Cloud SQL Proxy localhost:5433
```

### 2. Staging en GCP
```
Frontend → https://dashboard-rrhh-staging-xxxxx.run.app
Backend → https://api-staging-xxxxx.run.app
Database → Cloud SQL (conexión directa)
```

### 3. Producción en GCP
```
Frontend → https://dashboard-rrhh.mass.cl (con dominio custom)
Backend → https://api.mass.cl
Database → Cloud SQL (conexión directa)
```

---

## 📋 Checklist de Despliegue

### Pre-requisitos
- [ ] Cuenta GCP activa
- [ ] Proyecto: `prd-sfh-it-bi-erbi`
- [ ] Cloud SQL funcionando con datos
- [ ] gcloud CLI instalado
- [ ] Docker Desktop instalado
- [ ] Permisos de despliegue

### Backend
- [ ] Dockerfile creado para cada microservicio
- [ ] Dependencia Cloud SQL Socket Factory agregada
- [ ] Perfil `cloudrun` configurado
- [ ] Secrets configurados en Secret Manager
- [ ] Tests pasando
- [ ] JAR compilado correctamente

### Frontend
- [ ] Dockerfile con nginx
- [ ] Variables de entorno configuradas
- [ ] Build de producción funcionando
- [ ] API URLs apuntando a Cloud Run
- [ ] CORS configurado en backend

### Database
- [ ] Tabla `usuarios` creada
- [ ] Tabla `empleado` con datos
- [ ] Usuarios de prueba insertados
- [ ] Permisos de conexión configurados
- [ ] Backup configurado

### Infraestructura
- [ ] Service Account con permisos
- [ ] Cloud SQL instances configuradas
- [ ] Secret Manager configurado
- [ ] Cloud Build triggers (opcional)
- [ ] Monitoreo configurado

---

## 🚨 Preocupaciones Comunes

### "¿Perderé la conexión a la base de datos?"
**NO.** Cloud SQL Proxy es SOLO para desarrollo local. En producción:
- Cloud Run se conecta DIRECTAMENTE a Cloud SQL usando socket Unix
- Más rápido y seguro que el proxy
- Sin necesidad de mantener procesos corriendo

### "¿Qué pasa con mis datos?"
**Nada.** Los datos están en Cloud SQL, no en tu máquina local. El despliegue solo afecta el código de aplicación.

### "¿El frontend seguirá funcionando?"
**Sí.** Solo necesitas actualizar las URLs de la API para apuntar a los servicios de Cloud Run en lugar de localhost.

### "¿Es caro?"
**No inicialmente.** Cloud Run tiene capa gratuita:
- 2 millones de requests/mes gratis
- 360,000 GB-seconds de compute gratis
- Solo pagas por lo que usas

---

## 🎯 Próximos Pasos Recomendados

1. **Primero:** Crear Dockerfile para microservice-employee
2. **Segundo:** Probar build local con Docker
3. **Tercero:** Deploy manual a Cloud Run (1 microservicio)
4. **Cuarto:** Verificar conexión a Cloud SQL
5. **Quinto:** Deploy frontend
6. **Sexto:** Automatizar con Cloud Build

---

## 📞 Comandos Útiles

```bash
# Ver servicios en Cloud Run
gcloud run services list --region=southamerica-west1

# Ver logs
gcloud run services logs read microservice-employee --region=southamerica-west1

# Describir servicio
gcloud run services describe microservice-employee --region=southamerica-west1

# Actualizar variables de entorno
gcloud run services update microservice-employee \
  --set-env-vars="NEW_VAR=value" \
  --region=southamerica-west1
```

---

## ✅ Resultado Final

```
📱 Frontend en Cloud Run
    ↓ HTTPS
🔧 Backend en Cloud Run (microservice-employee, microservice-user)
    ↓ Socket Unix (conexión directa)
🗄️ Cloud SQL PostgreSQL
```

**Sin necesidad de:**
- ❌ Cloud SQL Proxy
- ❌ Servidores corriendo 24/7 en tu máquina
- ❌ Preocuparte por escalabilidad
- ❌ Configurar HTTPS manualmente

---

¿Quieres que empecemos con el paso 1: crear los Dockerfiles?
