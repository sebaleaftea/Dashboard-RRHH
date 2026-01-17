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
                e.apellido_paterno,
                e.apellido_materno,
                e.sexo,
                e.fecha_nacimiento,
                e.nacionalidad,
                e.email
            FROM empleado e
            ORDER BY e.nombre, e.apellido_paterno
            LIMIT ? OFFSET ?
            """;
        return jdbcTemplate.queryForList(sql, size, offset);
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
                COALESCE(s.nombre, 'Sin sucursal') AS sucursalNombre,
                COALESCE(c.cargo, '') AS cargo,
                e.sexo,
                e.fecha_nacimiento
            FROM contrato c
            INNER JOIN empleado e ON c.empleado_id = e.id
            LEFT JOIN sucursal s ON c.sucursal_id = s.id
            WHERE (
                c.activo = true
            ) OR (
                (c.finiquitado IS NULL OR c.finiquitado = false)
                AND (c.hasta IS NULL OR c.hasta::date >= CURRENT_DATE)
                AND (c.desde IS NULL OR c.desde::date <= CURRENT_DATE)
            )
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
                e.nombre || ' ' || e.apellido_paterno || ' ' || COALESCE(e.apellido_materno, '') as empleado_nombre,
                e.rut as empleado_rut,
                c.tipo_contrato,
                tc.nombre as tipo_contrato_nombre,
                c.fecha_contratacion,
                c.desde,
                c.hasta,
                c.finiquitado,
                c.cargo,
                c.sueldo_base,
                cc.nombre as centro_costo_nombre,
                s.nombre as sucursal_nombre,
                c.activo
            FROM contrato c
            INNER JOIN empleado e ON c.empleado_id = e.id
            LEFT JOIN tipo_contrato tc ON c.tipo_contrato = tc.id
            LEFT JOIN centro_costo cc ON c.centro_costo_id = cc.id
            LEFT JOIN sucursal s ON c.sucursal_id = s.id
            WHERE 1=1
            """ + (activo != null ? " AND c.activo = ?" : "") + """
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
                e.apellido_paterno         AS "apellidoPaterno",
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
            WHERE (
                c.activo = true
            ) OR (
                (c.finiquitado IS NULL OR c.finiquitado = false)
                AND (c.hasta IS NULL OR c.hasta::date >= CURRENT_DATE)
                AND (c.desde IS NULL OR c.desde::date <= CURRENT_DATE)
            )
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
                     (activo != null ? " AND activo = ?" : "");
        
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
                e.nombre || ' ' || e.apellido_paterno as empleado_nombre,
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
                e.nombre || ' ' || e.apellido_paterno as empleado_nombre,
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
     * Serie diaria de vacaciones desde N días atrás hasta hoy.
     */
    @GetMapping("/metrics/vacaciones/daily")
    public List<Map<String, Object>> getVacacionesDaily(
            @RequestParam(defaultValue = "14") int days
    ) {
        LocalDate start = LocalDate.now().minusDays(Math.max(0, days - 1));
        String sql = """
            SELECT 
                v.desde::date AS fecha,
                COUNT(*) AS total
            FROM vacaciones v
            WHERE v.desde::date >= ?
            GROUP BY v.desde::date
            ORDER BY fecha
            """;
        return jdbcTemplate.queryForList(sql, Date.valueOf(start));
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
                e.nombre || ' ' || e.apellido_paterno as empleado_nombre,
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
                e.nombre || ' ' || e.apellido_paterno as empleado_nombre,
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
     * Serie diaria de licencias desde N días atrás hasta hoy.
     */
    @GetMapping("/metrics/licencias/daily")
    public List<Map<String, Object>> getLicenciasDaily(
            @RequestParam(defaultValue = "14") int days
    ) {
        LocalDate start = LocalDate.now().minusDays(Math.max(0, days - 1));
        String sql = """
            SELECT 
                l.desde::date AS fecha,
                COUNT(*) AS total
            FROM licencias l
            WHERE l.desde::date >= ?
            GROUP BY l.desde::date
            ORDER BY fecha
            """;
        return jdbcTemplate.queryForList(sql, Date.valueOf(start));
    }

    /**
     * GET /api/db/stats
     * Estadísticas generales
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Long totalEmpleados = safeCount("SELECT COUNT(*) FROM empleado");
        Long totalContratos = safeCount("SELECT COUNT(*) FROM contrato");
        Long contratosActivos = safeCount("SELECT COUNT(*) FROM contrato WHERE activo = true");
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
