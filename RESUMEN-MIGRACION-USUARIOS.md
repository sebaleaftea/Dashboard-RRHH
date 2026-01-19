# Migración Completa: Sistema de Usuarios a Cloud SQL PostgreSQL

## ✅ Estado: COMPLETADO

### Cambios Realizados

#### Backend (microservice-user)
- ✅ Dependencia PostgreSQL agregada al `pom.xml`
- ✅ Configuración `application-cloudsql.properties` creada
- ✅ Entidad `User` actualizada con nuevos campos:
  - `username` (único, requerido)
  - `password` (en lugar de passwordHash)
  - `nombre`, `email`, `rol`, `activo`
  - `fecha_creacion`, `ultimo_acceso`
- ✅ Tabla renombrada: `users` → `usuarios`
- ✅ Repository, Service y Controller actualizados
- ✅ Validación de usuario activo implementada
- ✅ Actualización automática de `ultimo_acceso` al login

#### Frontend (React)
- ✅ Login.jsx actualizado para enviar `username` y `password`
- ✅ Compatible con la nueva estructura del backend

#### Scripts y Documentación
- ✅ `run-user-cloudsql.ps1` - Script para ejecutar con Cloud SQL
- ✅ `test-user-integration.ps1` - Script de pruebas
- ✅ `insert-usuarios-prueba.sql` - Usuarios de ejemplo
- ✅ `CLOUD-SQL-MIGRATION.md` - Documentación completa

---

## 🚀 Cómo Probar

### 1. Iniciar Cloud SQL Proxy
```powershell
cd c:\Workspace\Dashboard RRHH\scripts
.\start-cloud-sql-proxy.ps1
```

### 2. Insertar Usuarios de Prueba

**Opción A: Usar psql**
```powershell
# Conectar a Cloud SQL vía proxy
psql -h localhost -p 5433 -U grh-bdduser -d grh

# Ejecutar el script
\i 'c:\Workspace\Dashboard RRHH\scripts\insert-usuarios-prueba.sql'
```

**Opción B: Copiar y pegar en Cloud SQL Studio**
Abre `insert-usuarios-prueba.sql` y ejecuta las queries directamente.

### 3. Ejecutar microservice-user
```powershell
cd c:\Workspace\Dashboard RRHH\scripts
.\run-user-cloudsql.ps1
```

### 4. Ejecutar Tests
```powershell
.\test-user-integration.ps1
```

### 5. Ejecutar Frontend
```powershell
cd c:\Workspace\Dashboard RRHH\Front\dashboard-rrhh
npm run dev
```

### 6. Probar Login
- URL: http://localhost:5173
- Usuario: `admin`
- Password: `admin123`

---

## 📋 Credenciales de Prueba

| Username | Password | Rol | Activo |
|----------|----------|-----|--------|
| admin | admin123 | ADMIN | ✓ |
| usuario1 | user123 | USER | ✓ |
| rrhh | rrhh123 | RRHH | ✓ |
| inactivo | test123 | USER | ✗ |

---

## 🔍 Verificación

### Verificar Microservicio
```powershell
Invoke-WebRequest -Uri http://localhost:8081/api/user/all -UseBasicParsing
```

### Probar Login desde PowerShell
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

---

## ⚠️ Notas Importantes

### Seguridad - BCrypt (Próximo Paso Recomendado)

Actualmente las contraseñas se guardan en **texto plano**. Para producción:

1. **Agregar dependencia** en `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

2. **Actualizar UserService**:
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

// Al crear usuario
user.setPassword(passwordEncoder.encode(user.getPassword()));

// Al validar login
if (passwordEncoder.matches(plainPassword, user.getPassword())) {
    // Login exitoso
}
```

### Migrar Usuarios desde Laragon

Si tienes usuarios en Laragon MySQL que quieres migrar:

1. Exportar desde MySQL:
```sql
SELECT id, name as username, password_hash as password, 
       name as nombre, email, 'USER' as rol, 1 as activo
FROM users;
```

2. Adaptar el formato a la nueva tabla `usuarios`
3. Importar a Cloud SQL PostgreSQL

---

## 📊 Estructura de la Tabla

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

---

## 🎯 Próximos Pasos Sugeridos

1. ✅ **COMPLETADO** - Migrar sistema de autenticación a Cloud SQL
2. ⏭️ **Implementar BCrypt** para hash de contraseñas
3. ⏭️ **Agregar JWT** para tokens de sesión
4. ⏭️ **Implementar roles y permisos** en frontend
5. ⏭️ **Página de gestión de usuarios** (CRUD)
6. ⏭️ **Recuperación de contraseña** vía email

---

## 📞 Troubleshooting

### Error: "Relation 'usuarios' does not exist"
- Verifica que creaste la tabla usuarios en Cloud SQL
- Ejecuta: `CREATE TABLE usuarios ...` (ver CLOUD-SQL-MIGRATION.md)

### Error: "Connection refused port 5433"
- Verifica que Cloud SQL Proxy esté corriendo
- Ejecuta: `Get-Process | Where-Object {$_.ProcessName -eq "cloud-sql-proxy"}`

### Login falla con usuario correcto
- Verifica que el usuario tenga `activo = true`
- Verifica la contraseña (case sensitive)
- Revisa logs del microservicio

### Frontend no conecta con backend
- Verifica que microservice-user esté en puerto 8081
- Revisa CORS en UserController
- Verifica que el perfil `cloudsql` esté activo

---

## ✨ Resultado Final

- ✅ Autenticación centralizada en Cloud SQL PostgreSQL
- ✅ Compatible con MySQL local para desarrollo
- ✅ Frontend y backend sincronizados
- ✅ Validación de usuarios activos/inactivos
- ✅ Tracking de último acceso
- ✅ Sistema de roles implementado
- ✅ Scripts de prueba y documentación completa

**¡Sistema listo para usar!** 🎉
