# Guía de Migración - microservice-user a Cloud SQL PostgreSQL

## Cambios Realizados

### 1. Dependencias (pom.xml)
- ✅ Agregada dependencia de PostgreSQL: `org.postgresql:postgresql`
- ✅ Mantenida dependencia de MySQL para desarrollo local

### 2. Configuración
- ✅ **application.properties** - Configuración por defecto (MySQL local)
- ✅ **application-cloudsql.properties** - Nuevo perfil para Cloud SQL PostgreSQL
- ✅ **application-dev.properties** - Perfil de desarrollo (MySQL)
- ✅ **application-test.properties** - Perfil de testing (MySQL)

### 3. Modelo de Datos (User.java)
**Cambios en la entidad User:**
- ❌ Eliminado: `name`, `passwordHash`
- ✅ Agregado: `username`, `password`, `nombre`, `email`, `rol`, `activo`, `fecha_creacion`, `ultimo_acceso`
- ✅ Tabla renombrada: `users` → `usuarios`
- ✅ Agregado método `@PrePersist` para establecer `fecha_creacion` automáticamente

### 4. Repositorio (UserRepository.java)
- ✅ Método actualizado: `findByName()` → `findByUsername()`

### 5. Servicio (UserService.java)
- ✅ Método actualizado: `findByName()` → `findByUsername()`
- ✅ Login actualizado para usar `username` y `password`
- ✅ Validación de usuario activo agregada
- ✅ Actualización automática de `ultimo_acceso` al hacer login

### 6. Controlador (UserController.java)
- ✅ Endpoint `/login` actualizado para usar `username` y `password`
- ✅ Mensajes de error mejorados

## Cómo Ejecutar

### Opción 1: Con Cloud SQL (Recomendado)

#### Paso 1: Iniciar Cloud SQL Proxy
```powershell
cd c:\Workspace\Dashboard RRHH\scripts
.\start-cloud-sql-proxy.ps1
```

#### Paso 2: Ejecutar microservice-user con Cloud SQL
```powershell
cd c:\Workspace\Dashboard RRHH\scripts
.\run-user-cloudsql.ps1
```

### Opción 2: Con MySQL Local (Laragon)

```powershell
cd c:\Workspace\Dashboard RRHH\Backend\microservice-user
.\mvnw spring-boot:run
```

## Verificación

### 1. Verificar que el microservicio esté corriendo
```powershell
Invoke-WebRequest -Uri http://localhost:8081/api/user/all -UseBasicParsing
```

### 2. Probar el login
```powershell
$body = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

Invoke-WebRequest -Uri http://localhost:8081/api/user/login `
    -Method POST `
    -Body $body `
    -ContentType "application/json" `
    -UseBasicParsing
```

## Estructura de la Tabla `usuarios` en Cloud SQL

```sql
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100),
    email VARCHAR(100),
    rol VARCHAR(20) NOT NULL DEFAULT 'USER',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);
```

## Ejemplo de Usuario de Prueba

```sql
INSERT INTO usuarios (username, password, nombre, email, rol, activo) 
VALUES ('admin', 'admin123', 'Administrador', 'admin@empresa.cl', 'ADMIN', true);
```

## Próximos Pasos (Recomendado)

### 1. Implementar BCrypt para Passwords
Actualmente las contraseñas se guardan en texto plano. Se recomienda:

```java
// Agregar dependencia en pom.xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

// Usar en el servicio
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

// Al crear usuario
user.setPassword(passwordEncoder.encode(user.getPassword()));

// Al validar login
if (passwordEncoder.matches(plainPassword, user.getPassword())) {
    // Login exitoso
}
```

### 2. Migrar Usuarios de Laragon a Cloud SQL

```powershell
# Exportar desde Laragon MySQL
cd c:\laragon\bin\mysql\mysql-x.x.x\bin
.\mysqldump.exe -u root rrhh_massdb users > users_backup.sql

# Importar a Cloud SQL (ajustar formato de tabla users → usuarios)
# Ejecutar script de migración adaptado
```

### 3. Actualizar Frontend
El frontend debe enviar `username` y `password` en lugar de `name` y `passwordHash`:

```javascript
// Antes
{ name: "admin", passwordHash: "admin123" }

// Ahora
{ username: "admin", password: "admin123" }
```

## Troubleshooting

### Error: "Relation 'users' does not exist"
- El microservicio está buscando la tabla `users` en lugar de `usuarios`
- Verificar que el perfil `cloudsql` esté activo: `echo $env:SPRING_PROFILES_ACTIVE`

### Error: "Connection refused"
- Verificar que Cloud SQL Proxy esté corriendo en puerto 5433
- Ejecutar: `Get-Process | Where-Object {$_.ProcessName -eq "cloud-sql-proxy"}`

### Error: "Authentication failed"
- Verificar las credenciales en las variables de entorno
- Verificar que el usuario exista en la base de datos `grh`

## Variables de Entorno

```powershell
# Cloud SQL
$env:SPRING_PROFILES_ACTIVE = "cloudsql"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "grh"
$env:DB_USERNAME = "grh-bdduser"
$env:DB_PASSWORD = 'c_}2ysUR"6dXEk]o'
```
