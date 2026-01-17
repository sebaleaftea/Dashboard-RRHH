package com.microservice_employee.service;

import com.microservice_employee.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EtlService {
    private final JdbcTemplate jdbc;
    private final TalanaService talana;

    public EtlService(JdbcTemplate jdbcTemplate, TalanaService talanaService) {
        this.jdbc = jdbcTemplate;
        this.talana = talanaService;
    }

    public Map<String, Object> runInitialLoad(boolean onlyActives) {
        Map<String, Object> out = new LinkedHashMap<>();

        int cc = upsertCentroCostos();
        int sc = upsertSucursales();
        out.put("centro_costo_upserts", cc);
        out.put("sucursal_upserts", sc);

        int empleadosUpserts = 0;
        int contratosUpserts = 0;

        // Pre-cargar mapas para resolución rápida
        Map<String, Integer> ccByCodigo = loadCentroCostoByCodigo();
        Map<String, Integer> sucByNombre = loadSucursalByNombre();

        // Contratos activos por defecto (snapshot principal)
        java.util.List<ContratoResumenDTO> contratos = onlyActives
                ? talana.listarContratosActivos(null, null, true)
                : talana.listarContratosHistorico(null, null, 100);

        for (ContratoResumenDTO c : contratos) {
            if (c == null) continue;
            // empleado
            empleadosUpserts += upsertEmpleadoBasico(c);

            // resolver FK de centro costo por código (si viene)
            Integer ccId = null;
            if (c.getCentroCostoCodigo() != null && !c.getCentroCostoCodigo().isBlank()) {
                ccId = ccByCodigo.get(c.getCentroCostoCodigo());
            }
            // resolver sucursal por nombre (best-effort)
            Integer sucId = null;
            if (c.getSucursalNombre() != null && !c.getSucursalNombre().isBlank()) {
                sucId = sucByNombre.get(c.getSucursalNombre());
            }
            contratosUpserts += upsertContrato(c, ccId, sucId);
        }

        out.put("empleado_upserts", empleadosUpserts);
        out.put("contrato_upserts", contratosUpserts);
        out.put("total_contratos_procesados", contratos.size());
        return out;
    }

    private int upsertCentroCostos() {
        List<CentroCostoDTO> list = talana.obtenerCentroCostos(null, null);
        int count = 0;
        if (list == null) return 0;
        for (CentroCostoDTO c : list) {
            if (c == null || c.getId() == null) continue;
            int updated = jdbc.update(
                    "insert into centro_costo (id, codigo, nombre) values (?, ?, ?) " +
                            "on conflict (id) do update set codigo = excluded.codigo, nombre = excluded.nombre",
                    c.getId(), n(c.getCodigo()), n(c.getNombre())
            );
            count += updated > 0 ? 1 : 0;
        }
        return count;
    }

    private int upsertSucursales() {
        List<SucursalDTO> list = talana.obtenerSucursales(null, null);
        int count = 0;
        if (list == null) return 0;
        for (SucursalDTO s : list) {
            if (s == null || s.getId() == null) continue;
            int updated = jdbc.update(
                    "insert into sucursal (id, nombre) values (?, ?) " +
                            "on conflict (id) do update set nombre = excluded.nombre",
                    s.getId(), n(s.getNombre())
            );
            count += updated > 0 ? 1 : 0;
        }
        return count;
    }

    private Map<String, Integer> loadCentroCostoByCodigo() {
        return jdbc.query("select id, codigo from centro_costo where codigo is not null",
                rs -> {
                    Map<String, Integer> m = new HashMap<>();
                    while (rs.next()) m.put(rs.getString("codigo"), rs.getInt("id"));
                    return m;
                });
    }

    private Map<String, Integer> loadSucursalByNombre() {
        return jdbc.query("select id, nombre from sucursal where nombre is not null",
                rs -> {
                    Map<String, Integer> m = new HashMap<>();
                    while (rs.next()) m.put(rs.getString("nombre"), rs.getInt("id"));
                    return m;
                });
    }

    private int upsertEmpleadoBasico(ContratoResumenDTO c) {
        if (c.getEmpleadoId() == null) return 0;
        return jdbc.update(
                "insert into empleado (id, rut, nombre, ap_paterno) values (?, ?, ?, ?) " +
                        "on conflict (id) do update set rut = excluded.rut, nombre = excluded.nombre, ap_paterno = excluded.ap_paterno",
                c.getEmpleadoId().intValue(), n(c.getRut()), n(c.getNombre()), n(c.getApellidoPaterno())
        );
    }

    private int upsertContrato(ContratoResumenDTO c, Integer centroCostoId, Integer sucursalId) {
        if (c.getContratoId() == null || c.getEmpleadoId() == null) return 0;
        Boolean vigente = true; // snapshot de activos
        java.sql.Date fechaContratacion = c.getFechaContratacion() != null ? java.sql.Date.valueOf(c.getFechaContratacion()) : null;
        return jdbc.update(
                "insert into contrato (id, empleado_id, cargo, centro_costo_id, sucursal_id, jefe_nombre, fecha_contratacion, vigente) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "on conflict (id) do update set empleado_id=excluded.empleado_id, cargo=excluded.cargo, centro_costo_id=excluded.centro_costo_id, " +
                        "sucursal_id=excluded.sucursal_id, jefe_nombre=excluded.jefe_nombre, fecha_contratacion=excluded.fecha_contratacion, vigente=excluded.vigente",
                c.getContratoId(), c.getEmpleadoId().intValue(), n(c.getCargo()), centroCostoId, sucursalId, n(c.getJefeNombre()), fechaContratacion, vigente
        );
    }

    private static String n(String s) { return (s == null || s.isBlank()) ? null : s; }
}
