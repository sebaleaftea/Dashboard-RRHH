package com.microservice_employee.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.sql.Date;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/db")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long safeCount(String sql, Object... args) {
        try {
            if (args != null && args.length > 0) {
                return jdbcTemplate.queryForObject(sql, Long.class, args);
            } else {
                return jdbcTemplate.queryForObject(sql, Long.class);
            }
        } catch (Exception ex) {
            return 0L;
        }
    }

    /**
     * GET /api/db/empleados
     * Obtiene todos los empleados desde la base de datos
     */
    @GetMapping("/empleados")
    public List<Map<String, Object>> getEmpleados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int offset = page * size;
        String sql = """
            SELECT 
                e.id,
                e.rut,
                e.nombre,
                e.ap_paterno,
                e.ap_materno,
                e.sexo,
                e.fecha_nac,
                e.discapacidad
            FROM empleado e
            ORDER BY e.nombre, e.ap_paterno
            LIMIT ? OFFSET ?
            """;
        return jdbcTemplate.queryForList(sql, size, offset);
    }

    /**
     * GET /api/db/empleados/{empleadoId}/detalle
     * Devuelve detalle laboral del empleado a partir del último contrato vigente.
     */
    @GetMapping("/empleados/{empleadoId}/detalle")
    public Map<String, Object> getEmpleadoDetalle(@PathVariable int empleadoId) {
        String sql = """
            SELECT DISTINCT ON (c.empleado_id)
                COALESCE(c.cargo, '')              AS cargo,
                cc.codigo                          AS centroCostoCodigo,
                cc.nombre                          AS centroCostoNombre,
                COALESCE(s.nombre, 'Sin sucursal') AS sucursal,
                c.fecha_contratacion               AS fechaIngreso,
                ''                                 AS jefe
            FROM contrato c
            LEFT JOIN centro_costo cc ON c.centro_costo_id = cc.id
            LEFT JOIN sucursal s ON c.sucursal_id = s.id
            WHERE c.empleado_id = ? 
              AND c.vigente = true
            ORDER BY c.empleado_id, c.fecha_contratacion DESC NULLS LAST
            """;
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, empleadoId);
            return list.isEmpty() ? Map.of() : list.get(0);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    /**
     * GET /api/db/empleados/{empleadoId}/vacaciones
     */
    @GetMapping("/empleados/{empleadoId}/vacaciones")
    public List<Map<String, Object>> getVacacionesPorEmpleado(@PathVariable int empleadoId) {
        String sql = """
            SELECT 
                v.id,
                v.empleado_id,
                v.desde,
                v.hasta,
                v.retorno,
                v.dias,
                v.medios_dias,
                v.fecha_aprobacion,
                v.tipo
            FROM vacaciones v
            WHERE v.empleado_id = ?
            ORDER BY v.desde DESC
            """;
        return jdbcTemplate.queryForList(sql, empleadoId);
    }

    /**
     * GET /api/db/empleados/{empleadoId}/licencias
     */
    @GetMapping("/empleados/{empleadoId}/licencias")
    public List<Map<String, Object>> getLicenciasPorEmpleado(@PathVariable int empleadoId) {
        String sql = """
            SELECT 
                l.id,
                l.empleado_id,
                l.desde,
                l.hasta,
                l.dias,
                l.tipo,
                l.fecha_solicitud
            FROM licencias l
            WHERE l.empleado_id = ?
            ORDER BY l.desde DESC
            """;
        return jdbcTemplate.queryForList(sql, empleadoId);
    }

    /**
     * GET /api/db/empleados/activos
     * Devuelve una lista normalizada para el front con datos de empleados con contrato vigente.
     */
    @GetMapping("/empleados/activos")
    public List<Map<String, Object>> getEmpleadosActivos() {
        String sql = """
            SELECT DISTINCT ON (c.empleado_id)
                c.empleado_id,
                COALESCE(s.nombre, 'Sin sucursal') AS "sucursalNombre",
                COALESCE(c.cargo, '') AS cargo,
                e.sexo,
                e.fecha_nac AS fecha_nacimiento,
                e.discapacidad
            FROM contrato c
            INNER JOIN empleado e ON c.empleado_id = e.id
            LEFT JOIN sucursal s ON c.sucursal_id = s.id
            WHERE c.vigente = true
            ORDER BY c.empleado_id, c.fecha_contratacion DESC NULLS LAST
            """;
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    /**
     * GET /api/db/contratos
     * Obtiene todos los contratos activos desde la base de datos
     */
    @GetMapping("/contratos")
    public List<Map<String, Object>> getContratos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Boolean activo
    ) {
        int offset = page * size;
        String sql = """
            SELECT 
                c.id,
                c.empleado_id,
                e.nombre || ' ' || e.ap_paterno || ' ' || COALESCE(e.ap_materno, '') as empleado_nombre,
                e.rut as empleado_rut,
                c.fecha_contratacion,
                c.desde,
                c.hasta,
                c.cargo,
                c.cargo_norm,
                cc.nombre as centro_costo_nombre,
                s.nombre as sucursal_nombre,
                c.vigente
            FROM contrato c
            INNER JOIN empleado e ON c.empleado_id = e.id
            LEFT JOIN centro_costo cc ON c.centro_costo_id = cc.id
            LEFT JOIN sucursal s ON c.sucursal_id = s.id
            WHERE 1=1
            """ + (activo != null ? " AND c.vigente = ?" : "") + """
            ORDER BY c.fecha_contratacion DESC, e.nombre
            LIMIT ? OFFSET ?
            """;
        
        if (activo != null) {
            return jdbcTemplate.queryForList(sql, activo, size, offset);
        } else {
            return jdbcTemplate.queryForList(sql, size, offset);
        }
    }

    /**
     * GET /api/db/contratos/activos
     * Lista normalizada de contratos activos/vigentes con nombres en camelCase para el front.
     */
    @GetMapping("/contratos/activos")
    public List<Map<String, Object>> getContratosActivos() {
        String sql = """
            SELECT DISTINCT ON (c.empleado_id)
                c.empleado_id              AS "empleadoId",
                e.rut                      AS "rut",
                e.nombre                   AS "nombre",
                e.ap_paterno               AS "apellidoPaterno",
                COALESCE(c.cargo, '')      AS "cargo",
                cc.codigo                  AS "centroCostoCodigo",
                cc.nombre                  AS "centroCostoNombre",
                COALESCE(s.nombre, 'Sin sucursal') AS "sucursalNombre",
                ''                         AS "jefeNombre",
                c.fecha_contratacion       AS "fechaContratacion"
            FROM contrato c
            INNER JOIN empleado e ON c.empleado_id = e.id
            LEFT JOIN centro_costo cc ON c.centro_costo_id = cc.id
            LEFT JOIN sucursal s ON c.sucursal_id = s.id
            WHERE c.vigente = true
            ORDER BY c.empleado_id, c.fecha_contratacion DESC NULLS LAST
            """;
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    /**
     * GET /api/db/contratos/count
     * Cuenta total de contratos
     */
    @GetMapping("/contratos/count")
    public Map<String, Object> getContratosCount(@RequestParam(required = false) Boolean activo) {
        String sql = "SELECT COUNT(*) as total FROM contrato WHERE 1=1" + 
                     (activo != null ? " AND vigente = ?" : "");
        
        Long count;
        if (activo != null) {
            count = jdbcTemplate.queryForObject(sql, Long.class, activo);
        } else {
            count = jdbcTemplate.queryForObject(sql, Long.class);
        }
        
        return Map.of("total", count);
    }

    /**
     * GET /api/db/centros-costo
     * Obtiene todos los centros de costo
     */
    @GetMapping("/centros-costo")
    public List<Map<String, Object>> getCentrosCosto() {
        String sql = "SELECT id, nombre, codigo FROM centro_costo ORDER BY nombre";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * GET /api/db/sucursales
     * Obtiene todas las sucursales
     */
    @GetMapping("/sucursales")
    public List<Map<String, Object>> getSucursales() {
        String sql = "SELECT id, nombre, codigo FROM sucursal ORDER BY nombre";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * GET /api/db/tipos-contrato
     * Obtiene todos los tipos de contrato
     */
    @GetMapping("/tipos-contrato")
    public List<Map<String, Object>> getTiposContrato() {
        String sql = "SELECT id, nombre FROM tipo_contrato ORDER BY nombre";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * GET /api/db/vacaciones
     * Obtiene todas las vacaciones (tabla para llenado manual)
     */
    @GetMapping("/vacaciones")
    public List<Map<String, Object>> getVacaciones() {
        String sql = """
            SELECT 
                v.id,
                v.empleado_id,
                e.nombre || ' ' || e.ap_paterno as empleado_nombre,
                e.rut as empleado_rut,
                v.desde,
                v.hasta,
                v.retorno,
                v.dias,
                v.medios_dias,
                v.fecha_aprobacion,
                v.tipo
            FROM vacaciones v
            INNER JOIN empleado e ON v.empleado_id = e.id
            ORDER BY v.desde DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * GET /api/db/vacaciones/rango
     * Lista vacaciones en un rango de fechas [desde, hasta]
     */
    @GetMapping("/vacaciones/rango")
    public List<Map<String, Object>> getVacacionesRango(
            @RequestParam String desde,
            @RequestParam String hasta
    ) {
        String sql = """
            SELECT 
                v.id,
                v.empleado_id,
                e.nombre || ' ' || e.ap_paterno as empleado_nombre,
                e.rut as empleado_rut,
                v.desde,
                v.hasta,
                v.retorno,
                v.dias,
                v.medios_dias,
                v.fecha_aprobacion,
                v.tipo
            FROM vacaciones v
            INNER JOIN empleado e ON v.empleado_id = e.id
            WHERE v.desde::date >= DATE ? AND v.desde::date <= DATE ?
            ORDER BY v.desde DESC
            """;
        return jdbcTemplate.queryForList(sql, desde, hasta);
    }

    /**
     * GET /api/db/metrics/vacaciones/daily
     * Serie diaria de vacaciones activas desde ayer hasta los próximos N días.
     * Cuenta todas las vacaciones que estén vigentes en cada fecha (no solo las que inician ese día).
     */
    @GetMapping("/metrics/vacaciones/daily")
    public List<Map<String, Object>> getVacacionesDaily(
            @RequestParam(defaultValue = "14") int days
    ) {
        LocalDate ayer = LocalDate.now().minusDays(1);
        LocalDate fin = ayer.plusDays(days);
        String fechasSql = """
            WITH RECURSIVE fechas AS (
                SELECT ?::date AS fecha
                UNION ALL
                SELECT fecha + 1
                FROM fechas
                WHERE fecha < ?::date
            )
            SELECT fecha FROM fechas ORDER BY fecha
        """;
        List<Map<String, Object>> fechas = jdbcTemplate.queryForList(fechasSql, Date.valueOf(ayer), Date.valueOf(fin));
        // Para cada fecha, obtener total y personas
        String personasSql = """
            SELECT 
                e.nombre || ' ' || e.ap_paterno as nombre,
                e.rut as rut,
                COALESCE(s.nombre, 'Sin sucursal') as sucursal,
                v.desde,
                COALESCE(v.hasta, v.retorno) as hasta
            FROM vacaciones v
            INNER JOIN empleado e ON v.empleado_id = e.id
            LEFT JOIN LATERAL (
                SELECT c.sucursal_id
                FROM contrato c
                WHERE c.empleado_id = e.id AND c.vigente = true
                ORDER BY c.fecha_contratacion DESC NULLS LAST
                LIMIT 1
            ) contrato_vigente ON true
            LEFT JOIN sucursal s ON contrato_vigente.sucursal_id = s.id
            WHERE ?::date >= v.desde::date AND ?::date <= COALESCE(v.hasta::date, v.retorno::date)
        """;
        return fechas.stream().map(f -> {
            LocalDate fecha = ((Date) f.get("fecha")).toLocalDate();
            List<Map<String, Object>> personas = jdbcTemplate.queryForList(personasSql, Date.valueOf(fecha), Date.valueOf(fecha));
            return Map.of(
                "fecha", fecha.toString(),
                "total", personas.size(),
                "personas", personas
            );
        }).toList();
    }

    /**
     * GET /api/db/licencias
     * Obtiene todas las licencias (tabla para llenado manual)
     */
    @GetMapping("/licencias")
    public List<Map<String, Object>> getLicencias() {
        String sql = """
            SELECT 
                l.id,
                l.empleado_id,
                e.nombre || ' ' || e.ap_paterno as empleado_nombre,
                e.rut as empleado_rut,
                l.desde,
                l.hasta,
                l.dias,
                l.tipo,
                l.fecha_solicitud
            FROM licencias l
            INNER JOIN empleado e ON l.empleado_id = e.id
            ORDER BY l.desde DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * GET /api/db/licencias/rango
     * Lista licencias en un rango de fechas [desde, hasta]
     */
    @GetMapping("/licencias/rango")
    public List<Map<String, Object>> getLicenciasRango(
            @RequestParam String desde,
            @RequestParam String hasta
    ) {
        String sql = """
            SELECT 
                l.id,
                l.empleado_id,
                e.nombre || ' ' || e.ap_paterno as empleado_nombre,
                e.rut as empleado_rut,
                l.desde,
                l.hasta,
                l.dias,
                l.tipo,
                l.fecha_solicitud
            FROM licencias l
            INNER JOIN empleado e ON l.empleado_id = e.id
            WHERE l.desde::date >= DATE ? AND l.desde::date <= DATE ?
            ORDER BY l.desde DESC
            """;
        return jdbcTemplate.queryForList(sql, desde, hasta);
    }

    /**
     * GET /api/db/metrics/licencias/daily
     * Serie diaria de licencias activas desde ayer hasta los próximos N días.
     * Cuenta todas las licencias que estén vigentes en cada fecha (no solo las que inician ese día).
     */
    @GetMapping("/metrics/licencias/daily")
    public List<Map<String, Object>> getLicenciasDaily(
            @RequestParam(defaultValue = "14") int days
    ) {
        LocalDate ayer = LocalDate.now().minusDays(1);
        LocalDate fin = ayer.plusDays(days);
        String fechasSql = """
            WITH RECURSIVE fechas AS (
                SELECT ?::date AS fecha
                UNION ALL
                SELECT fecha + 1
                FROM fechas
                WHERE fecha < ?::date
            )
            SELECT fecha FROM fechas ORDER BY fecha
        """;
        List<Map<String, Object>> fechas = jdbcTemplate.queryForList(fechasSql, Date.valueOf(ayer), Date.valueOf(fin));
        String personasSql = """
            SELECT 
                e.nombre || ' ' || e.ap_paterno as nombre,
                e.rut as rut,
                COALESCE(s.nombre, 'Sin sucursal') as sucursal,
                l.desde,
                l.hasta
            FROM licencias l
            INNER JOIN empleado e ON l.empleado_id = e.id
            LEFT JOIN LATERAL (
                SELECT c.sucursal_id
                FROM contrato c
                WHERE c.empleado_id = e.id AND c.vigente = true
                ORDER BY c.fecha_contratacion DESC NULLS LAST
                LIMIT 1
            ) contrato_vigente ON true
            LEFT JOIN sucursal s ON contrato_vigente.sucursal_id = s.id
            WHERE ?::date >= l.desde::date AND ?::date <= l.hasta::date
        """;
        return fechas.stream().map(f -> {
            LocalDate fecha = ((Date) f.get("fecha")).toLocalDate();
            List<Map<String, Object>> personas = jdbcTemplate.queryForList(personasSql, Date.valueOf(fecha), Date.valueOf(fecha));
            return Map.of(
                "fecha", fecha.toString(),
                "total", personas.size(),
                "personas", personas
            );
        }).toList();
    }

    /**
     * GET /api/db/stats
     * Estadísticas generales
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Long totalEmpleados = safeCount("SELECT COUNT(*) FROM empleado");
        Long totalContratos = safeCount("SELECT COUNT(*) FROM contrato");
        Long contratosActivos = safeCount("SELECT COUNT(*) FROM contrato WHERE vigente = true");
        Long totalVacaciones = safeCount("SELECT COUNT(*) FROM vacaciones");
        Long totalLicencias = safeCount("SELECT COUNT(*) FROM licencias");

        // Último día (hoy) para métricas diarias rápidas
        LocalDate hoy = LocalDate.now();
        Long vacacionesHoy = safeCount(
            "SELECT COUNT(*) FROM vacaciones WHERE desde::date = ?", Date.valueOf(hoy));
        Long licenciasHoy = safeCount(
            "SELECT COUNT(*) FROM licencias WHERE desde::date = ?", Date.valueOf(hoy));
        
        return Map.of(
            "totalEmpleados", totalEmpleados,
            "totalContratos", totalContratos,
            "contratosActivos", contratosActivos,
            "totalVacaciones", totalVacaciones,
            "totalLicencias", totalLicencias,
            "vacacionesHoy", vacacionesHoy,
            "licenciasHoy", licenciasHoy
        );
    }
}
