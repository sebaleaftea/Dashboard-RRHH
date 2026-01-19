-- Script para cambiar el campo discapacidad de boolean a text
-- Esto permitirá almacenar la descripción de la discapacidad desde Talana

-- 1. Crear una columna temporal para el texto
ALTER TABLE empleado ADD COLUMN discapacidad_texto TEXT;

-- 2. Copiar los valores actuales (convertir boolean a texto descriptivo)
UPDATE empleado 
SET discapacidad_texto = CASE 
    WHEN discapacidad = true THEN 'Discapacidad registrada'
    ELSE NULL 
END;

-- 3. Eliminar la columna boolean
ALTER TABLE empleado DROP COLUMN discapacidad;

-- 4. Renombrar la columna temporal
ALTER TABLE empleado RENAME COLUMN discapacidad_texto TO discapacidad;

-- 5. Crear índice para búsquedas rápidas
CREATE INDEX idx_empleado_discapacidad ON empleado(discapacidad) WHERE discapacidad IS NOT NULL;

-- Verificar el cambio
SELECT 
    COUNT(*) FILTER (WHERE discapacidad IS NOT NULL) as con_discapacidad,
    COUNT(*) FILTER (WHERE discapacidad IS NULL) as sin_discapacidad
FROM empleado;
