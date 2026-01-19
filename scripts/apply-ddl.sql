-- Aplicar DDL a Cloud SQL PostgreSQL
-- Ejecutar con: gcloud sql connect bdd-grh --user=grh-bdduser --database=grh --quiet
-- O con psql: psql -h 34.176.82.205 -U grh-bdduser -d grh -f apply-ddl.sql

-- Empleados
CREATE TABLE IF NOT EXISTS empleado (
  id               INT PRIMARY KEY,
  rut              TEXT UNIQUE,
  nombre           TEXT,
  ap_paterno       TEXT,
  ap_materno       TEXT,
  sexo             CHAR(1),
  fecha_nac        DATE,
  discapacidad     BOOLEAN
);

-- Centro de costo
CREATE TABLE IF NOT EXISTS centro_costo (
  id      INT PRIMARY KEY,
  codigo  TEXT UNIQUE,
  nombre  TEXT
);

-- Sucursal
CREATE TABLE IF NOT EXISTS sucursal (
  id      INT PRIMARY KEY,
  nombre  TEXT
);

-- Contratos
CREATE TABLE IF NOT EXISTS contrato (
  id                 BIGINT PRIMARY KEY,
  empleado_id        INT REFERENCES empleado(id),
  cargo              TEXT,
  cargo_norm         TEXT,
  centro_costo_id    INT REFERENCES centro_costo(id),
  sucursal_id        INT REFERENCES sucursal(id),
  jefe_nombre        TEXT,
  fecha_contratacion DATE,
  vigente            BOOLEAN,
  desde              DATE,
  hasta              DATE
);

-- Ausencias
CREATE TABLE IF NOT EXISTS ausencia (
  id           BIGINT PRIMARY KEY,
  empleado_id  INT REFERENCES empleado(id),
  tipo         TEXT,
  desde        DATE,
  hasta        DATE,
  aprobada     BOOLEAN,
  dias         NUMERIC,
  medios_dias  INT,
  estado       TEXT,
  motivo_codigo TEXT,
  fecha_retorno DATE
);

-- Tipos de ausencia
CREATE TABLE IF NOT EXISTS tipo_ausencia (
  codigo       TEXT PRIMARY KEY,
  descripcion  TEXT
);

-- Vacaciones
CREATE TABLE IF NOT EXISTS vacaciones (
  id                BIGINT PRIMARY KEY,
  empleado_id       INT REFERENCES empleado(id),
  desde             DATE,
  hasta             DATE,
  retorno           DATE,
  dias              NUMERIC,
  medios_dias       INT,
  fecha_aprobacion  TIMESTAMP,
  tipo              TEXT
);

-- Licencias (ausencias médicas, permisos, etc.)
CREATE TABLE IF NOT EXISTS licencias (
  id                BIGINT PRIMARY KEY,
  empleado_id       INT REFERENCES empleado(id),
  desde             DATE,
  hasta             DATE,
  dias              NUMERIC,
  tipo              TEXT,
  fecha_solicitud   TIMESTAMP
);

-- Índices para empleado
CREATE INDEX IF NOT EXISTS idx_empleado_rut ON empleado(rut);

-- Índices para contrato
CREATE INDEX IF NOT EXISTS idx_contrato_empleado ON contrato(empleado_id);
CREATE INDEX IF NOT EXISTS idx_contrato_vigente ON contrato(vigente);
CREATE INDEX IF NOT EXISTS idx_contrato_cc ON contrato(centro_costo_id);
CREATE INDEX IF NOT EXISTS idx_contrato_sucursal ON contrato(sucursal_id);

-- Índices para ausencia
CREATE INDEX IF NOT EXISTS idx_ausencia_emp ON ausencia(empleado_id);
CREATE INDEX IF NOT EXISTS idx_ausencia_rango ON ausencia(desde, hasta);

-- Índices para vacaciones
CREATE INDEX IF NOT EXISTS idx_vacaciones_emp ON vacaciones(empleado_id);
CREATE INDEX IF NOT EXISTS idx_vacaciones_rango ON vacaciones(desde, hasta);

-- Índices para licencias
CREATE INDEX IF NOT EXISTS idx_licencias_emp ON licencias(empleado_id);
CREATE INDEX IF NOT EXISTS idx_licencias_rango ON licencias(desde, hasta);

-- Verificar tablas creadas
\dt

-- Mostrar esquema de empleado
\d empleado
