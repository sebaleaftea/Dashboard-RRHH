-- Script para insertar usuarios de prueba en Cloud SQL
-- Ejecutar después de crear la tabla usuarios

-- Usuario Administrador
INSERT INTO usuarios (username, password, nombre, email, rol, activo) 
VALUES ('admin', 'admin123', 'Administrador Sistema', 'admin@mass.cl', 'ADMIN', true);

-- Usuario Regular
INSERT INTO usuarios (username, password, nombre, email, rol, activo) 
VALUES ('usuario1', 'user123', 'Juan Pérez', 'juan.perez@mass.cl', 'USER', true);

-- Usuario RRHH
INSERT INTO usuarios (username, password, nombre, email, rol, activo) 
VALUES ('rrhh', 'rrhh123', 'María González', 'maria.gonzalez@mass.cl', 'RRHH', true);

-- Usuario Inactivo (para pruebas de validación)
INSERT INTO usuarios (username, password, nombre, email, rol, activo) 
VALUES ('inactivo', 'test123', 'Usuario Inactivo', 'inactivo@mass.cl', 'USER', false);

-- Verificar usuarios insertados
SELECT 
    id, 
    username, 
    nombre, 
    email, 
    rol, 
    activo, 
    fecha_creacion, 
    ultimo_acceso 
FROM usuarios 
ORDER BY id;
