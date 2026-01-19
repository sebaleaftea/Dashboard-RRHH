-- Esquema base para snapshot RRHH
create table if not exists empleado (
  id               int primary key,
  rut              text unique,
  nombre           text,
  ap_paterno       text,
  ap_materno       text,
  sexo             char(1),
  fecha_nac        date,
  discapacidad     boolean
);

create table if not exists centro_costo (
  id      int primary key,
  codigo  text unique,
  nombre  text
);

create table if not exists sucursal (
  id      int primary key,
  nombre  text
);

create table if not exists contrato (
  id                 bigint primary key,
  empleado_id        int references empleado(id),
  cargo              text,
  cargo_norm         text,
  centro_costo_id    int references centro_costo(id),
  sucursal_id        int references sucursal(id),
  jefe_nombre        text,
  fecha_contratacion date,
  vigente            boolean,
  desde              date,
  hasta              date
);

create table if not exists ausencia (
  id           bigint primary key,
  empleado_id  int references empleado(id),
  tipo         text,
  desde        date,
  hasta        date,
  aprobada     boolean,
  dias         numeric,
  medios_dias  int,
  estado       text,
  motivo_codigo text,
  fecha_retorno date
);

-- Índices
create index if not exists idx_empleado_rut on empleado(rut);
create index if not exists idx_contrato_empleado on contrato(empleado_id);
create index if not exists idx_contrato_vigente on contrato(vigente);
create index if not exists idx_contrato_cc on contrato(centro_costo_id);
create index if not exists idx_contrato_sucursal on contrato(sucursal_id);
create index if not exists idx_ausencia_emp on ausencia(empleado_id);
create index if not exists idx_ausencia_rango on ausencia(desde, hasta);

-- Tipos de ausencia (catálogo)
create table if not exists tipo_ausencia (
  codigo       text primary key,
  descripcion  text
);

-- Vacaciones
create table if not exists vacaciones (
  id                bigint primary key,
  empleado_id       int references empleado(id),
  desde             date,
  hasta             date,
  retorno           date,
  dias              numeric,
  medios_dias       int,
  fecha_aprobacion  timestamp,
  tipo              text
);

create index if not exists idx_vacaciones_emp on vacaciones(empleado_id);
create index if not exists idx_vacaciones_rango on vacaciones(desde, hasta);
