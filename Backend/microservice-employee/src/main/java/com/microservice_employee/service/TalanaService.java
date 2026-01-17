package com.microservice_employee.service;

import com.microservice_employee.dto.*;
import com.microservice_employee.exception.TalanaUpstreamException;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("talana")
public class TalanaService {
    private final TalanaOkHttpService talanaOkHttpService;
    private final ObjectMapper objectMapper;
    @org.springframework.beans.factory.annotation.Value("${talana.vacaciones.skip-saldo:true}")
    private boolean skipSaldoVacaciones;

    // Cache simple para evitar golpear Talana repetidamente y mitigar 429
    private volatile List<EmpleadoSimpleDTO> cacheEmpleadosBasicos = null;
    private volatile long cacheTimestampMs = 0L;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos

    private static final long DETAILS_CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutos
    private final Map<String, CacheEntry<ContratoDTO>> cacheContratoPorRut = new ConcurrentHashMap<>();
    private final Map<Integer, CacheEntry<ContratoDTO>> cacheContratoPorEmpleadoId = new ConcurrentHashMap<>();
    private final Map<Integer, CacheEntry<VacacionesDTO>> cacheVacacionesPorEmpleadoId = new ConcurrentHashMap<>();
    private final Map<Integer, CacheEntry<List<LicenciaDTO>>> cacheLicenciasPorEmpleadoId = new ConcurrentHashMap<>();
    private final Map<Integer, CacheEntry<List<VacacionesResumenDTO>>> cacheVacacionesResumenPorEmpleadoId = new ConcurrentHashMap<>();
    private final Map<Integer, CacheEntry<EmpleadoDTO>> cacheDetallePersonaPorId = new ConcurrentHashMap<>();

    private volatile List<CentroCostoDTO> cacheCentroCostos = null;
    private volatile long cacheCentroCostosTimestampMs = 0L;
    private volatile List<SucursalDTO> cacheSucursales = null;
    private volatile long cacheSucursalesTimestampMs = 0L;
    private volatile List<JobTitleDTO> cacheJobTitles = null;
    private volatile long cacheJobTitlesTimestampMs = 0L;
    private static final long CATALOG_CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutos
    private volatile List<ContratoResumenDTO> cacheContratosActivos = null;
    private volatile long cacheContratosActivosTimestampMs = 0L;
    private static final long CONTRATOS_ACTIVOS_TTL_MS = 5 * 60 * 1000; // 5 minutos

    private static final class CacheEntry<T> {
        private final T value;
        private final long timestampMs;

        private CacheEntry(T value, long timestampMs) {
            this.value = value;
            this.timestampMs = timestampMs;
        }
    }

    public TalanaService(TalanaOkHttpService talanaOkHttpService, ObjectMapper objectMapper) {
        this.talanaOkHttpService = talanaOkHttpService;
        // Use the Spring-managed ObjectMapper and ensure Java time support
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public List<EmpleadoDTO> obtenerEmpleadosCompletos() {
        List<EmpleadoSimpleDTO> empleadosBasicos = obtenerEmpleadosBasicos();
        return empleadosBasicos.stream()
                .map(this::enriquecerEmpleado)
                .collect(Collectors.toList());
    }

    public List<EmpleadoDTO> obtenerEmpleadosCompletos(int limit, int offset) {
        List<EmpleadoSimpleDTO> empleadosBasicos = obtenerEmpleadosBasicos(limit, offset);
        return empleadosBasicos.stream()
                .map(this::enriquecerEmpleado)
                .collect(Collectors.toList());
    }

    public List<CentroCostoDTO> obtenerCentroCostos(Integer limit, Integer offset) {
        long now = System.currentTimeMillis();
        boolean isDefault = (limit == null && offset == null);
        if (isDefault && cacheCentroCostos != null && (now - cacheCentroCostosTimestampMs) < CATALOG_CACHE_TTL_MS) {
            return cacheCentroCostos;
        }
        try {
            String json = talanaOkHttpService.getCentroCostos(limit, offset);
            List<CentroCostoDTO> list = parseList(json, CentroCostoDTO[].class);
            if (isDefault) {
                cacheCentroCostos = list;
                cacheCentroCostosTimestampMs = now;
            }
            return list;
        } catch (IOException e) {
            if (cacheCentroCostos != null) {
                return cacheCentroCostos;
            }
            throw asUpstreamException(e);
        }
    }

    public List<SucursalDTO> obtenerSucursales(Integer limit, Integer offset) {
        long now = System.currentTimeMillis();
        boolean isDefault = (limit == null && offset == null);
        if (isDefault && cacheSucursales != null && (now - cacheSucursalesTimestampMs) < CATALOG_CACHE_TTL_MS) {
            return cacheSucursales;
        }
        try {
            String json = talanaOkHttpService.getSucursales(limit, offset);
            List<SucursalDTO> list = parseList(json, SucursalDTO[].class);
            if (isDefault) {
                cacheSucursales = list;
                cacheSucursalesTimestampMs = now;
            }
            return list;
        } catch (IOException e) {
            if (cacheSucursales != null) {
                return cacheSucursales;
            }
            throw asUpstreamException(e);
        }
    }

    public List<JobTitleDTO> obtenerJobTitles(Integer limit, Integer offset) {
        long now = System.currentTimeMillis();
        boolean isDefault = (limit == null && offset == null);
        if (isDefault && cacheJobTitles != null && (now - cacheJobTitlesTimestampMs) < CATALOG_CACHE_TTL_MS) {
            return cacheJobTitles;
        }
        try {
            String json = talanaOkHttpService.getJobTitles(limit, offset);
            List<JobTitleDTO> list = parseList(json, JobTitleDTO[].class);
            if (isDefault) {
                cacheJobTitles = list;
                cacheJobTitlesTimestampMs = now;
            }
            return list;
        } catch (IOException e) {
            if (cacheJobTitles != null) {
                return cacheJobTitles;
            }
            throw asUpstreamException(e);
        }
    }

    /**
     * Lista contratos históricos paginando desde Talana, devolviendo sólo campos clave para el frontend.
     * Equivalente al script Python pero como endpoint de backend.
     */
    public List<ContratoResumenDTO> listarContratosHistorico(String searchSince, String searchTo, Integer pageSize) {
        String since = (searchSince == null || searchSince.isBlank()) ? "20240101" : searchSince.trim();
        String to = (searchTo == null || searchTo.isBlank()) ? java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) : searchTo.trim();
        int size = (pageSize == null || pageSize < 1) ? 100 : pageSize;

        List<ContratoResumenDTO> out = new ArrayList<>();
        int page = 1;
        while (true) {
            Map<String, String> qp = new HashMap<>();
            qp.put("show", "all");
            qp.put("page", String.valueOf(page));
            qp.put("page_size", String.valueOf(size));
            qp.put("search_since", since);
            qp.put("search_to", to);

            String json;
            try {
                json = talanaOkHttpService.getContratosPaginados(qp);
            } catch (IOException e) {
                throw asUpstreamException(e);
            }

            if (json == null || json.isBlank()) {
                break;
            }
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode results = root.get("results");
                if (results == null || !results.isArray() || results.size() == 0) {
                    break;
                }
                for (JsonNode item : results) {
                    out.add(mapContratoToResumen(item));
                }
                // avanzar si hay "next"
                JsonNode next = root.get("next");
                if (next == null || next.isNull() || (next.isTextual() && next.asText().isBlank())) {
                    break;
                }
                page++;
            } catch (IOException e) {
                throw asUpstreamException(e);
            }
        }
        return out;
    }

    /**
     * Lista contratos activos (vigentes) y devuelve campos clave para UI.
     * Si pageSize es nulo, recorre todas las páginas con page_size=100.
     */
    public List<ContratoResumenDTO> listarContratosActivos(Integer page, Integer pageSize) {
        return listarContratosActivos(page, pageSize, false);
    }

    /**
     * Variante con control de forzar refresco (ignora caché cuando forceRefresh=true).
     */
    public List<ContratoResumenDTO> listarContratosActivos(Integer page, Integer pageSize, boolean forceRefresh) {
        int size = (pageSize == null || pageSize < 1) ? 100 : pageSize;
        List<ContratoResumenDTO> out = new ArrayList<>();

        int currentPage = (page == null || page < 1) ? 1 : page;
        boolean iterateAll = (page == null);

        // Cache only when iterating all pages (full snapshot)
        if (iterateAll && !forceRefresh) {
            long now = System.currentTimeMillis();
            if (cacheContratosActivos != null && (now - cacheContratosActivosTimestampMs) < CONTRATOS_ACTIVOS_TTL_MS) {
                return cacheContratosActivos;
            }
        }

        while (true) {
            Map<String, String> qp = new HashMap<>();
            qp.put("type_status_search", "actives");
            qp.put("show_version", "last");
            qp.put("page", String.valueOf(currentPage));
            qp.put("page_size", String.valueOf(size));

            String json;
            try {
                json = talanaOkHttpService.getContratosPaginados(qp);
            } catch (IOException e) {
                throw asUpstreamException(e);
            }

            if (json == null || json.isBlank()) {
                break;
            }
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode results = root.get("results");
                if (results == null || !results.isArray() || results.size() == 0) {
                    break;
                }
                for (JsonNode item : results) {
                    out.add(mapContratoToResumen(item));
                }
                if (!iterateAll) {
                    break;
                }
                JsonNode next = root.get("next");
                if (next == null || next.isNull() || (next.isTextual() && next.asText().isBlank())) {
                    break;
                }
                currentPage++;
            } catch (IOException e) {
                throw asUpstreamException(e);
            }
        }
        // Store snapshot in cache if complete list was requested
        if (iterateAll) {
            cacheContratosActivos = out;
            cacheContratosActivosTimestampMs = System.currentTimeMillis();
        }
        return out;
    }

    private ContratoResumenDTO mapContratoToResumen(JsonNode item) {
        ContratoResumenDTO dto = new ContratoResumenDTO();
        // ids
        dto.setContratoId(asLong(item.get("id")));
        dto.setEmpleadoId(asLong(item.get("empleado")));

        // empleadoDetails
        JsonNode empleadoDetails = item.get("empleadoDetails");
        if (empleadoDetails != null && empleadoDetails.isObject()) {
            dto.setNombre(asText(empleadoDetails.get("nombre")));
            dto.setRut(asText(empleadoDetails.get("rut")));
            dto.setApellidoPaterno(asText(empleadoDetails.get("apellidoPaterno")));
        }

        // campos principales
        dto.setCargo(asText(item.get("cargo")));

        JsonNode centroCosto = item.get("centroCosto");
        if (centroCosto != null && centroCosto.isObject()) {
            dto.setCentroCostoCodigo(asText(centroCosto.get("codigo")));
            dto.setCentroCostoNombre(asText(centroCosto.get("nombre")));
        }

        JsonNode sucursal = item.get("sucursal");
        if (sucursal != null && sucursal.isObject()) {
            dto.setSucursalNombre(asText(sucursal.get("nombre")));
        }

        JsonNode jefe = item.get("jefe");
        if (jefe != null) {
            if (jefe.isObject()) {
                dto.setJefeNombre(asText(jefe.get("nombre")));
            } else {
                dto.setJefeNombre(asText(jefe));
            }
        }

        // fechaContratacion o desde
        String fechaStr = asText(item.get("fechaContratacion"));
        if (fechaStr == null || fechaStr.isBlank()) {
            fechaStr = asText(item.get("desde"));
        }
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                // intenta ISO (yyyy-MM-dd), si no, deja null
                dto.setFechaContratacion(java.time.LocalDate.parse(fechaStr));
            } catch (Exception ignored) {
                dto.setFechaContratacion(null);
            }
        }
        return dto;
    }

    private static Long asLong(JsonNode n) {
        if (n == null) return null;
        if (n.isNumber()) return n.longValue();
        if (n.isTextual()) {
            try { return Long.parseLong(n.asText()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String asText(JsonNode n) {
        return (n == null || n.isNull()) ? null : n.asText();
    }

    public EmpleadoDTO obtenerDetallePersona(Integer personaId) {
        if (personaId == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        CacheEntry<EmpleadoDTO> cached = cacheDetallePersonaPorId.get(personaId);
        if (cached != null && (now - cached.timestampMs) < DETAILS_CACHE_TTL_MS) {
            return cached.value;
        }

        try {
            String json = talanaOkHttpService.getPersonaDetalle(personaId);
            if (json == null || json.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(json);
            EmpleadoDTO detalle = parsePersonaDetalle(root);

            // En Talana, /persona/{id}/ suele traer datos personales; los datos laborales suelen estar en /contrato/{id}/.
            // Si faltan columnas clave, hacemos fallback interno a contrato (sin exponerlo al frontend).
            if (detalle != null && needsContratoFallback(detalle)) {
                ContratoDTO contrato = obtenerContratoPorEmpleadoId(personaId);
                if (contrato != null) {
                    if (detalle.getCargo() == null || detalle.getCargo().isBlank()) {
                        detalle.setCargo(blankToNull(contrato.getCargo()));
                    }
                    if ((detalle.getCodigoCentroCosto() == null || detalle.getCodigoCentroCosto().isBlank())
                            || (detalle.getNombreCentroCosto() == null || detalle.getNombreCentroCosto().isBlank())) {
                        CentroCostoDTO cc = contrato.getCentroCosto();
                        if (cc != null) {
                            if (detalle.getCodigoCentroCosto() == null || detalle.getCodigoCentroCosto().isBlank()) {
                                detalle.setCodigoCentroCosto(blankToNull(cc.getCodigo()));
                            }
                            if (detalle.getNombreCentroCosto() == null || detalle.getNombreCentroCosto().isBlank()) {
                                detalle.setNombreCentroCosto(blankToNull(cc.getNombre()));
                            }
                        }
                    }
                    if (detalle.getSucursal() == null || detalle.getSucursal().isBlank()) {
                        SucursalDTO s = contrato.getSucursal();
                        if (s != null) {
                            detalle.setSucursal(blankToNull(s.getNombre()));
                        }
                    }
                    if (detalle.getFechaIngreso() == null && contrato.getFechaContratacion() != null) {
                        detalle.setFechaIngreso(contrato.getFechaContratacion());
                    }
                    if (detalle.getJefe() == null && contrato.getJefe() != null) {
                        detalle.setJefe(contrato.getJefe());
                    }
                }
            }

            // Si aún faltan datos laborales, intentamos descubrirlos en otros endpoints comunes.
            if (detalle != null && needsContratoFallback(detalle)) {
                boolean applied = tryApplyDatosLaboralesFromTalana(detalle);
                if (!applied && needsContratoFallback(detalle)) {
                    System.out.println("[TalanaService] Faltan campos laborales tras consulta: "
                            + missingLaboralFields(detalle) + " personaId=" + personaId + " rut=" + detalle.getRut());
                }
            }

            if (detalle != null) {
                cacheDetallePersonaPorId.put(personaId, new CacheEntry<>(detalle, now));
            }
            return detalle;
        } catch (IOException e) {
            System.out.println("[TalanaService] Error obteniendo detalle personaId=" + personaId + ": " + e.getMessage());
            throw asUpstreamException(e);
        }
    }

    private boolean tryApplyDatosLaboralesFromTalana(EmpleadoDTO detalle) {
        if (detalle == null || detalle.getId() == null) {
            return false;
        }
        Integer personaId = detalle.getId();
        String rut = detalle.getRut();

        List<String> paths = new ArrayList<>();

        // Variantes típicas (algunas instalaciones usan plural)
        paths.add("/contrato/" + personaId + "/");
        paths.add("/contratos/" + personaId + "/");
        paths.add("/contrato/?persona=" + personaId);
        paths.add("/contrato/?empleado=" + personaId);
        paths.add("/contratos/?persona=" + personaId);
        paths.add("/contratos/?empleado=" + personaId);
        paths.add("/persona/" + personaId + "/contrato/");
        paths.add("/persona/" + personaId + "/contratos/");

        if (rut != null && !rut.isBlank()) {
            String encodedRut = URLEncoder.encode(rut.trim(), StandardCharsets.UTF_8);
            paths.add("/contrato/?rut=" + encodedRut);
            paths.add("/contratos/?rut=" + encodedRut);
        }

        // Otras rutas posibles (dependen de cuenta/versión)
        paths.add("/empleado/" + personaId + "/");
        paths.add("/empleados/" + personaId + "/");
        paths.add("/relacion-laboral/" + personaId + "/");
        paths.add("/relacion-laboral/?persona=" + personaId);

        for (String path : paths) {
            try {
                String json = talanaOkHttpService.getFromTalana(path);
                JsonNode obj = extractFirstObject(json);
                if (obj == null) {
                    continue;
                }
                boolean applied = applyDatosLaboralesFromNode(detalle, obj);
                if (applied && !needsContratoFallback(detalle)) {
                    return true;
                }
                // si aplicó algo, igual consideramos éxito parcial
                if (applied) {
                    return true;
                }
            } catch (IOException e) {
                // 404 esperado en algunos endpoints: seguimos probando sin ruido
                if (!isHttpStatus(e, 404)) {
                    System.out.println("[TalanaService] Error consultando Talana path=" + path + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    private JsonNode extractFirstObject(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            JsonNode arr = objectMapper.readTree(trimmed);
            if (arr != null && arr.isArray() && arr.size() > 0 && arr.get(0).isObject()) {
                return arr.get(0);
            }
            return null;
        }
        JsonNode root = objectMapper.readTree(trimmed);
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isObject()) {
            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0 && results.get(0).isObject()) {
                return results.get(0);
            }
            JsonNode data = root.get("data");
            if (data != null && data.isArray() && data.size() > 0 && data.get(0).isObject()) {
                return data.get(0);
            }
            return root;
        }
        return null;
    }

    private boolean applyDatosLaboralesFromNode(EmpleadoDTO detalle, JsonNode node) {
        if (detalle == null || node == null || node.isNull()) {
            return false;
        }
        boolean changed = false;

        // cargo
        if (detalle.getCargo() == null || detalle.getCargo().isBlank()) {
            String cargo = readString(node, "cargo", "jobTitle", "job_title", "jobTitleName", "job_title_name", "tituloCargo");
            if (cargo == null) {
                cargo = deepReadString(node, 4, "cargo", "jobTitleName", "job_title_name", "jobTitle", "job_title");
            }
            cargo = blankToNull(cargo);
            if (cargo != null) {
                detalle.setCargo(cargo);
                changed = true;
            }
        }

        // centro costo
        if ((detalle.getCodigoCentroCosto() == null || detalle.getCodigoCentroCosto().isBlank())
                || (detalle.getNombreCentroCosto() == null || detalle.getNombreCentroCosto().isBlank())) {
            JsonNode ccNode = firstObjectNode(node, "centroCosto", "centro_costo");
            if (ccNode == null) {
                ccNode = deepFindObjectByKey(node, 4, "centroCosto", "centro_costo");
            }
            if (ccNode != null) {
                String codigo = blankToNull(readString(ccNode, "codigo", "code"));
                String nombre = blankToNull(readString(ccNode, "nombre", "name"));
                if (codigo != null && (detalle.getCodigoCentroCosto() == null || detalle.getCodigoCentroCosto().isBlank())) {
                    detalle.setCodigoCentroCosto(codigo);
                    changed = true;
                }
                if (nombre != null && (detalle.getNombreCentroCosto() == null || detalle.getNombreCentroCosto().isBlank())) {
                    detalle.setNombreCentroCosto(nombre);
                    changed = true;
                }
            }
        }

        // sucursal
        if (detalle.getSucursal() == null || detalle.getSucursal().isBlank()) {
            JsonNode sucNode = firstObjectNode(node, "sucursal", "branch");
            if (sucNode == null) {
                sucNode = deepFindObjectByKey(node, 4, "sucursal", "branch");
            }
            String suc = null;
            if (sucNode != null) {
                suc = readString(sucNode, "nombre", "name");
            }
            suc = blankToNull(suc);
            if (suc != null) {
                detalle.setSucursal(suc);
                changed = true;
            }
        }

        // fecha ingreso
        if (detalle.getFechaIngreso() == null) {
            String fecha = readString(node, "fechaContratacion", "fecha_contratacion", "fechaIngreso", "fecha_ingreso", "hiringDate", "hire_date");
            if (fecha == null) {
                fecha = deepReadString(node, 4, "fechaContratacion", "fecha_contratacion", "fechaIngreso", "fecha_ingreso", "hiringDate", "hire_date", "fechaInicio", "fecha_inicio");
            }
            fecha = blankToNull(fecha);
            if (fecha != null) {
                try {
                    detalle.setFechaIngreso(java.time.LocalDate.parse(fecha));
                    changed = true;
                } catch (Exception ignored) {
                }
            }
        }

        // jefe
        if (detalle.getJefe() == null) {
            JsonNode jefeNode = firstObjectNode(node, "jefe", "manager", "supervisor", "jefeDirecto", "jefe_directo");
            if (jefeNode == null) {
                jefeNode = deepFindObjectByKey(node, 4, "jefe", "manager", "supervisor", "jefeDirecto", "jefe_directo");
            }
            if (jefeNode != null) {
                EmpleadoSimpleDTO jefe = new EmpleadoSimpleDTO();
                Integer jefeId = readInt(jefeNode, "id");
                if (jefeId != null) jefe.setId(jefeId);
                jefe.setRut(readString(jefeNode, "rut"));
                jefe.setNombre(readString(jefeNode, "nombre", "name"));
                jefe.setApellidoPaterno(readString(jefeNode, "apellidoPaterno", "apellido_paterno", "lastName", "lastname"));
                jefe.setApellidoMaterno(readString(jefeNode, "apellidoMaterno", "apellido_materno"));
                detalle.setJefe(jefe);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean needsContratoFallback(EmpleadoDTO detalle) {
        return detalle.getCargo() == null
                || detalle.getCodigoCentroCosto() == null
                || detalle.getNombreCentroCosto() == null
                || detalle.getSucursal() == null
                || detalle.getFechaIngreso() == null
                || detalle.getJefe() == null;
    }

    private static String missingLaboralFields(EmpleadoDTO d) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (d.getCargo() == null || d.getCargo().isBlank()) missing.add("cargo");
        if (d.getCodigoCentroCosto() == null || d.getCodigoCentroCosto().isBlank()) missing.add("codigoCentroCosto");
        if (d.getNombreCentroCosto() == null || d.getNombreCentroCosto().isBlank()) missing.add("nombreCentroCosto");
        if (d.getSucursal() == null || d.getSucursal().isBlank()) missing.add("sucursal");
        if (d.getFechaIngreso() == null) missing.add("fechaIngreso");
        if (d.getJefe() == null) missing.add("jefe");
        return missing.isEmpty() ? "(ninguno)" : missing.toString();
    }

    public List<EmpleadoSimpleDTO> obtenerEmpleadosBasicos() {
        return obtenerEmpleadosBasicos(null, null);
    }

    public List<EmpleadoSimpleDTO> obtenerEmpleadosBasicos(Integer limit, Integer offset) {
        try {
            // Si el cache está fresco, devolverlo
            long now = System.currentTimeMillis();
            boolean isDefaultPage = (limit == null && offset == null);
            if (isDefaultPage && cacheEmpleadosBasicos != null && (now - cacheTimestampMs) < CACHE_TTL_MS) {
                return cacheEmpleadosBasicos;
            }

            String json;
            if (limit != null && offset != null) {
                json = getPersonasPaginadasLimitOffset(limit, offset);
            } else {
                json = talanaOkHttpService.getPersonas();
            }
            if (json == null || json.isBlank()) {
                System.out.println("[TalanaService] JSON vacío o nulo desde Talana");
                return cacheEmpleadosBasicos != null ? cacheEmpleadosBasicos : Collections.emptyList();
            }
            System.out.println("[TalanaService] JSON recibido de Talana (len=" + json.length() + ")");

            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                EmpleadoSimpleDTO[] empleados = objectMapper.readValue(trimmed, EmpleadoSimpleDTO[].class);
                List<EmpleadoSimpleDTO> list = Arrays.asList(empleados);
                if (isDefaultPage) {
                    cacheEmpleadosBasicos = list;
                    cacheTimestampMs = now;
                }
                return list;
            } else {
                JsonNode root = objectMapper.readTree(trimmed);
                JsonNode results = root.get("results");
                if (results != null && results.isArray()) {
                    EmpleadoSimpleDTO[] empleados = objectMapper.readValue(results.toString(), EmpleadoSimpleDTO[].class);
                    List<EmpleadoSimpleDTO> list = Arrays.asList(empleados);
                    if (isDefaultPage) {
                        cacheEmpleadosBasicos = list;
                        cacheTimestampMs = now;
                    }
                    return list;
                }
                // fallback si el API retorna otra clave común como 'data'
                JsonNode data = root.get("data");
                if (data != null && data.isArray()) {
                    EmpleadoSimpleDTO[] empleados = objectMapper.readValue(data.toString(), EmpleadoSimpleDTO[].class);
                    List<EmpleadoSimpleDTO> list = Arrays.asList(empleados);
                    if (isDefaultPage) {
                        cacheEmpleadosBasicos = list;
                        cacheTimestampMs = now;
                    }
                    return list;
                }
                System.out.println("[TalanaService] Estructura JSON no reconocida para personas.");
                return cacheEmpleadosBasicos != null ? cacheEmpleadosBasicos : Collections.emptyList();
            }
        } catch (IOException e) {
            if (cacheEmpleadosBasicos != null) {
                return cacheEmpleadosBasicos;
            }
            throw asUpstreamException(e);
        }
    }

    private String getPersonasPaginadasLimitOffset(int limit, int offset) throws IOException {
        // Traducción a paginado Talana (page/page_size) para grandes volúmenes.
        // Soporte robusto si offset no es múltiplo de limit: recorta y, si falta, trae 1 página extra.
        int pageSize = Math.max(1, limit);
        // Talana recomienda 50-100; evitamos page_size enormes por performance.
        if (pageSize > 100) {
            pageSize = 100;
        }
        int safeOffset = Math.max(0, offset);

        int pageIndex0 = safeOffset / pageSize; // 0-based
        int remainder = safeOffset % pageSize;
        int page = pageIndex0 + 1; // Talana es 1-based

        String json = talanaOkHttpService.getPersonasPaginadas(page, pageSize);
        if (remainder == 0) {
            return json;
        }

        // Si hay remainder, reconstruimos un wrapper con results recortados (y completamos con la siguiente página si hace falta)
        JsonNode root = objectMapper.readTree(json);
        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) {
            // Si no viene en wrapper, devolvemos tal cual.
            return json;
        }

        List<JsonNode> merged = new ArrayList<>();
        for (int i = remainder; i < results.size(); i++) {
            merged.add(results.get(i));
        }

        if (merged.size() < pageSize) {
            String jsonNext = talanaOkHttpService.getPersonasPaginadas(page + 1, pageSize);
            JsonNode rootNext = objectMapper.readTree(jsonNext);
            JsonNode resultsNext = rootNext.get("results");
            if (resultsNext != null && resultsNext.isArray()) {
                for (int i = 0; i < resultsNext.size() && merged.size() < pageSize; i++) {
                    merged.add(resultsNext.get(i));
                }
            }
        }

        // Construir wrapper mínimo: { results: [...] }
        com.fasterxml.jackson.databind.node.ArrayNode arr = objectMapper.createArrayNode();
        for (JsonNode n : merged) {
            if (n != null && !n.isNull()) {
                arr.add(n);
            }
        }
        return objectMapper.createObjectNode()
                .set("results", arr)
                .toString();
    }

    private EmpleadoDTO enriquecerEmpleado(EmpleadoSimpleDTO empleadoBasico) {
        EmpleadoDTO empleadoCompleto = new EmpleadoDTO();
        BeanUtils.copyProperties(empleadoBasico, empleadoCompleto);

        // Detalle persona (sin contrato)
        EmpleadoDTO detalle = obtenerDetallePersona(empleadoBasico.getId());
        if (detalle != null) {
            empleadoCompleto.setCargo(detalle.getCargo());
            empleadoCompleto.setCodigoCentroCosto(detalle.getCodigoCentroCosto());
            empleadoCompleto.setNombreCentroCosto(detalle.getNombreCentroCosto());
            empleadoCompleto.setSucursal(detalle.getSucursal());
            empleadoCompleto.setFechaIngreso(detalle.getFechaIngreso());
            empleadoCompleto.setJefe(detalle.getJefe());
        }

        // Vacaciones
        VacacionesDTO vacaciones = obtenerVacaciones(empleadoBasico.getId());
        empleadoCompleto.setVacaciones(vacaciones);

        // Licencias
        List<LicenciaDTO> licencias = obtenerLicencias(empleadoBasico.getId());
        empleadoCompleto.setLicencias(licencias);

        return empleadoCompleto;
    }

    private EmpleadoDTO parsePersonaDetalle(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }

        EmpleadoDTO dto = new EmpleadoDTO();

        Integer id = readInt(root, "id", "personaId");
        if (id != null) {
            dto.setId(id);
        }

        // Datos básicos (útil para depuración y/o UI)
        dto.setRut(blankToNull(readString(root, "rut")));
        dto.setNombre(blankToNull(readString(root, "nombre")));
        dto.setApellidoPaterno(blankToNull(readString(root, "apellidoPaterno", "apellido_paterno")));
        dto.setApellidoMaterno(blankToNull(readString(root, "apellidoMaterno", "apellido_materno")));
        // Sexo directo si viene en el nodo principal
        String sexo = blankToNull(readString(root, "sexo"));
        dto.setSexo(sexo);
        // Discapacidad: usualmente en arreglo "detalles" -> campo "discapacidades" (string)
        Boolean tieneDiscapacidad = null;
        JsonNode detallesArr = root.get("detalles");
        if (detallesArr != null && detallesArr.isArray() && detallesArr.size() > 0) {
            for (JsonNode det : detallesArr) {
                String discapacidadesStr = blankToNull(readString(det, "discapacidades"));
                if (discapacidadesStr != null && !discapacidadesStr.isBlank()) {
                    tieneDiscapacidad = Boolean.TRUE;
                    break;
                }
            }
        }
        dto.setTieneDiscapacidad(tieneDiscapacidad);

        // Cargo: intentar texto directo o resolver por job-title id
        String cargo = readString(root, "cargo", "jobTitle", "job_title", "jobTitleName", "job_title_name", "tituloCargo", "jobTitleNombre", "job_title_nombre");
        if (cargo == null || cargo.isBlank()) {
            cargo = deepReadString(root, 6, "cargo", "jobTitle", "job_title", "jobTitleName", "job_title_name", "tituloCargo", "jobTitleNombre", "job_title_nombre", "jobTitleText", "job_title_text");
        }
        if ((cargo == null || cargo.isBlank())) {
            Integer jobTitleId = readInt(root, "jobTitleId", "job_title_id", "jobTitle", "job_title");
            if (jobTitleId == null) {
                jobTitleId = deepReadInt(root, 6, "jobTitleId", "job_title_id", "jobTitle", "job_title");
            }
            if (jobTitleId != null) {
                cargo = resolveJobTitleName(jobTitleId);
            }
        }
        dto.setCargo(blankToNull(cargo));

        // Centro costo: objeto o id (puede venir anidado)
        JsonNode centroCostoNode = firstObjectNode(root, "centroCosto", "centro_costo");
        if (centroCostoNode == null) {
            centroCostoNode = deepFindObjectByKey(root, 6, "centroCosto", "centro_costo");
        }
        String codigoCC = null;
        String nombreCC = null;
        if (centroCostoNode != null) {
            // si es objeto
            codigoCC = readString(centroCostoNode, "codigo", "code");
            nombreCC = readString(centroCostoNode, "nombre", "name");
        }
        if ((codigoCC == null || nombreCC == null)) {
            Integer centroCostoId = readInt(root, "centroCostoId", "centro_costo_id");
            if (centroCostoId == null) {
                centroCostoId = deepReadInt(root, 6, "centroCostoId", "centro_costo_id");
            }
            if (centroCostoId != null) {
                CentroCostoDTO cc = resolveCentroCosto(centroCostoId);
                if (cc != null) {
                    if (codigoCC == null) codigoCC = cc.getCodigo();
                    if (nombreCC == null) nombreCC = cc.getNombre();
                }
            }
        }
        dto.setCodigoCentroCosto(blankToNull(codigoCC));
        dto.setNombreCentroCosto(blankToNull(nombreCC));

        // Sucursal (solo nombre para tabla)
        String sucursalNombre = null;
        JsonNode sucursalNode = firstObjectNode(root, "sucursal", "branch");
        if (sucursalNode == null) {
            sucursalNode = deepFindObjectByKey(root, 6, "sucursal", "branch");
        }
        if (sucursalNode != null) {
            sucursalNombre = readString(sucursalNode, "nombre", "name");
        }
        if (sucursalNombre == null) {
            Integer sucursalId = readInt(root, "sucursalId", "sucursal_id");
            if (sucursalId == null) {
                sucursalId = deepReadInt(root, 6, "sucursalId", "sucursal_id");
            }
            if (sucursalId != null) {
                SucursalDTO s = resolveSucursal(sucursalId);
                if (s != null) {
                    sucursalNombre = s.getNombre();
                }
            }
        }
        dto.setSucursal(blankToNull(sucursalNombre));

        // Fecha ingreso
        String fecha = readString(root, "fechaIngreso", "fecha_ingreso", "fechaContratacion", "fecha_contratacion", "hiringDate", "hire_date");
        if (fecha == null || fecha.isBlank()) {
            fecha = deepReadString(root, 6, "fechaIngreso", "fecha_ingreso", "fechaContratacion", "fecha_contratacion", "hiringDate", "hire_date", "fechaInicio", "fecha_inicio");
        }
        if (fecha != null && !fecha.isBlank()) {
            try {
                dto.setFechaIngreso(java.time.LocalDate.parse(fecha));
            } catch (Exception ignored) {
                // ignorar si viene en otro formato
            }
        }

        // Jefe: objeto o string (puede venir anidado)
        JsonNode jefeNode = firstObjectNode(root, "jefe", "manager", "supervisor");
        if (jefeNode == null) {
            jefeNode = deepFindObjectByKey(root, 6, "jefe", "manager", "supervisor", "jefeDirecto", "jefe_directo");
        }
        if (jefeNode != null) {
            EmpleadoSimpleDTO jefe = new EmpleadoSimpleDTO();
            Integer jefeId = readInt(jefeNode, "id");
            if (jefeId != null) jefe.setId(jefeId);
            jefe.setRut(readString(jefeNode, "rut"));
            jefe.setNombre(readString(jefeNode, "nombre", "name"));
            jefe.setApellidoPaterno(readString(jefeNode, "apellidoPaterno", "apellido_paterno", "lastName", "lastname"));
            jefe.setApellidoMaterno(readString(jefeNode, "apellidoMaterno", "apellido_materno"));
            dto.setJefe(jefe);
        }

        return dto;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String resolveJobTitleName(int jobTitleId) {
        List<JobTitleDTO> titles = obtenerJobTitles(null, null);
        if (titles == null) return null;
        for (JobTitleDTO t : titles) {
            if (t != null && t.getId() != null && t.getId() == jobTitleId) {
                String name = t.getName();
                if (name == null || name.isBlank()) {
                    name = t.getNombre();
                }
                return name;
            }
        }
        return null;
    }

    private CentroCostoDTO resolveCentroCosto(int centroCostoId) {
        List<CentroCostoDTO> centros = obtenerCentroCostos(null, null);
        if (centros == null) return null;
        for (CentroCostoDTO c : centros) {
            if (c != null && c.getId() != null && c.getId() == centroCostoId) {
                return c;
            }
        }
        return null;
    }

    private SucursalDTO resolveSucursal(int sucursalId) {
        List<SucursalDTO> list = obtenerSucursales(null, null);
        if (list == null) return null;
        for (SucursalDTO s : list) {
            if (s != null && s.getId() != null && s.getId() == sucursalId) {
                return s;
            }
        }
        return null;
    }

    private static String readString(JsonNode node, String... keys) {
        if (node == null || keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            JsonNode child = node.get(key);
            if (child == null || child.isNull()) continue;
            if (child.isTextual()) {
                String v = child.asText();
                if (v != null && !v.isBlank()) return v;
            }
            if (child.isObject()) {
                // try common name fields inside object
                String v = readString(child, "nombre", "name", "title", "descripcion", "description");
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private static Integer readInt(JsonNode node, String... keys) {
        if (node == null || keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            JsonNode child = node.get(key);
            if (child == null || child.isNull()) continue;
            if (child.isInt() || child.isLong()) return child.asInt();
            if (child.isTextual()) {
                try {
                    return Integer.parseInt(child.asText().trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (child.isObject()) {
                JsonNode idNode = child.get("id");
                if (idNode != null && !idNode.isNull()) {
                    if (idNode.isInt() || idNode.isLong()) return idNode.asInt();
                    if (idNode.isTextual()) {
                        try {
                            return Integer.parseInt(idNode.asText().trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        return null;
    }

    private static JsonNode firstObjectNode(JsonNode node, String... keys) {
        if (node == null || keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            JsonNode child = node.get(key);
            if (child != null && child.isObject()) {
                return child;
            }
        }
        return null;
    }

    private static JsonNode deepFindObjectByKey(JsonNode node, int maxDepth, String... keys) {
        if (node == null || node.isNull() || maxDepth < 0 || keys == null) {
            return null;
        }
        Set<String> keySet = new HashSet<>(Arrays.asList(keys));
        return deepFindObjectByKey(node, maxDepth, keySet);
    }

    private static JsonNode deepFindObjectByKey(JsonNode node, int maxDepth, Set<String> keys) {
        if (node == null || node.isNull() || maxDepth < 0) {
            return null;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey();
                JsonNode v = e.getValue();
                if (k != null && keys.contains(k) && v != null && v.isObject()) {
                    return v;
                }
                JsonNode found = deepFindObjectByKey(v, maxDepth - 1, keys);
                if (found != null) return found;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = deepFindObjectByKey(child, maxDepth - 1, keys);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JsonNode deepFindByKey(JsonNode node, int maxDepth, String... keys) {
        if (node == null || node.isNull() || maxDepth < 0 || keys == null) {
            return null;
        }
        Set<String> keySet = new HashSet<>(Arrays.asList(keys));
        return deepFindByKey(node, maxDepth, keySet);
    }

    private static JsonNode deepFindByKey(JsonNode node, int maxDepth, Set<String> keys) {
        if (node == null || node.isNull() || maxDepth < 0) {
            return null;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey();
                JsonNode v = e.getValue();
                if (k != null && keys.contains(k)) {
                    return v;
                }
                JsonNode found = deepFindByKey(v, maxDepth - 1, keys);
                if (found != null) return found;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = deepFindByKey(child, maxDepth - 1, keys);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String deepReadString(JsonNode node, int maxDepth, String... keys) {
        JsonNode found = deepFindByKey(node, maxDepth, keys);
        if (found == null || found.isNull()) {
            return null;
        }
        if (found.isTextual()) {
            String v = found.asText();
            return (v == null || v.isBlank()) ? null : v;
        }
        if (found.isNumber() || found.isBoolean()) {
            String v = found.asText();
            return (v == null || v.isBlank()) ? null : v;
        }
        if (found.isObject()) {
            return readString(found, "nombre", "name", "title", "descripcion", "description");
        }
        return null;
    }

    private static Integer deepReadInt(JsonNode node, int maxDepth, String... keys) {
        JsonNode found = deepFindByKey(node, maxDepth, keys);
        if (found == null || found.isNull()) {
            return null;
        }
        if (found.isInt() || found.isLong()) {
            return found.asInt();
        }
        if (found.isTextual()) {
            try {
                return Integer.parseInt(found.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (found.isObject()) {
            return readInt(found, "id");
        }
        return null;
    }

    public ContratoDTO obtenerContratoPorEmpleadoId(Integer empleadoId) {
        if (empleadoId == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        CacheEntry<ContratoDTO> cached = cacheContratoPorEmpleadoId.get(empleadoId);
        if (cached != null && (now - cached.timestampMs) < DETAILS_CACHE_TTL_MS) {
            return cached.value;
        }
        try {
            // Según documentación Talana: para obtener jefe/datos laborales por persona,
            // primero listar contrato activo vía /contrato-paginado/ y luego obtener detalle por /contrato/{id}/.
            Map<String, String> qp = new HashMap<>();
            qp.put("empleado", String.valueOf(empleadoId));
            qp.put("type_status_search", "actives");
            qp.put("page", "1");
            qp.put("page_size", "1");
            qp.put("show_version", "last");

            String json = talanaOkHttpService.getContratosPaginados(qp);
            Long contratoId = extractFirstContratoId(json);
            String detailJson = (contratoId != null)
                    ? talanaOkHttpService.getContratoDetalle(contratoId)
                    : json;

            ContratoDTO value = parseContrato(detailJson);
            if (value != null) {
                cacheContratoPorEmpleadoId.put(empleadoId, new CacheEntry<>(value, now));
            }
            return value;
        } catch (IOException e) {
            // Algunos entornos no exponen /contrato/{id}/. Probamos variantes por query antes de rendirnos.
            if (isHttpStatus(e, 404)) {
                ContratoDTO value = tryContratoQueryFallbacks(empleadoId);
                if (value != null) {
                    cacheContratoPorEmpleadoId.put(empleadoId, new CacheEntry<>(value, now));
                }
                return value;
            }
            System.out.println("[TalanaService] Error obteniendo contrato para empleadoId=" + empleadoId + ": " + e.getMessage());
            throw asUpstreamException(e);
        }
    }

    private Long extractFirstContratoId(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                ContratoDTO[] contratos = objectMapper.readValue(trimmed, ContratoDTO[].class);
                if (contratos != null && contratos.length > 0 && contratos[0] != null) {
                    return contratos[0].getId();
                }
                return null;
            }

            JsonNode root = objectMapper.readTree(trimmed);
            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode first = results.get(0);
                if (first != null && first.isObject()) {
                    JsonNode id = first.get("id");
                    if (id != null && (id.isInt() || id.isLong())) {
                        return id.asLong();
                    }
                    if (id != null && id.isTextual()) {
                        try {
                            return Long.parseLong(id.asText().trim());
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ContratoDTO tryContratoQueryFallbacks(Integer empleadoId) {
        if (empleadoId == null) return null;
        String[] urls = new String[] {
                "/contrato/?empleado=" + empleadoId,
                "/contrato/?persona=" + empleadoId
        };
        for (String url : urls) {
            try {
                String json = talanaOkHttpService.getFromTalana(url);
                ContratoDTO value = parseContrato(json);
                if (value != null) {
                    return value;
                }
            } catch (IOException ignored) {
                // intentar siguiente
            }
        }
        return null;
    }

    private static boolean isHttpStatus(IOException ex, int statusCode) {
        if (ex == null || ex.getMessage() == null) {
            return false;
        }
        return ex.getMessage().startsWith("HTTP " + statusCode + " ");
    }

    public ContratoDTO obtenerContratoPorRut(String rut) {
        if (rut == null || rut.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        CacheEntry<ContratoDTO> cached = cacheContratoPorRut.get(rut);
        if (cached != null && (now - cached.timestampMs) < DETAILS_CACHE_TTL_MS) {
            return cached.value;
        }
        try {
            // Preferir contrato ACTIVO usando paginado oficial, luego obtener detalle
            String encodedRut = URLEncoder.encode(rut.trim(), StandardCharsets.UTF_8);
            Map<String, String> qp = new HashMap<>();
            qp.put("rut", encodedRut);
            qp.put("type_status_search", "actives");
            qp.put("page", "1");
            qp.put("page_size", "1");
            qp.put("show_version", "last");
            String json = talanaOkHttpService.getContratosPaginados(qp);
            Long contratoId = extractFirstContratoId(json);
            String detailJson = (contratoId != null)
                    ? talanaOkHttpService.getContratoDetalle(contratoId)
                    : json;
            ContratoDTO value = parseContrato(detailJson);
            if (value != null) {
                cacheContratoPorRut.put(rut, new CacheEntry<>(value, now));
            }
            return value;
        } catch (IOException e) {
            System.out.println("[TalanaService] Error obteniendo contrato para rut=" + rut + ": " + e.getMessage());
            throw asUpstreamException(e);
        }
    }

    private ContratoDTO parseContrato(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            ContratoDTO[] contratos = objectMapper.readValue(trimmed, ContratoDTO[].class);
            return (contratos != null && contratos.length > 0) ? contratos[0] : null;
        }

        JsonNode root = objectMapper.readTree(trimmed);
        // wrapper paginado
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            ContratoDTO[] contratos = objectMapper.readValue(results.toString(), ContratoDTO[].class);
            return (contratos != null && contratos.length > 0) ? contratos[0] : null;
        }
        JsonNode data = root.get("data");
        if (data != null && data.isArray()) {
            ContratoDTO[] contratos = objectMapper.readValue(data.toString(), ContratoDTO[].class);
            return (contratos != null && contratos.length > 0) ? contratos[0] : null;
        }

        // objeto directo
        return objectMapper.readValue(root.toString(), ContratoDTO.class);
    }

    private <T> List<T> parseList(String json, Class<T[]> arrayClass) throws IOException {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            T[] items = objectMapper.readValue(trimmed, arrayClass);
            return items != null ? Arrays.asList(items) : Collections.emptyList();
        }
        JsonNode root = objectMapper.readTree(trimmed);
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            T[] items = objectMapper.readValue(results.toString(), arrayClass);
            return items != null ? Arrays.asList(items) : Collections.emptyList();
        }
        JsonNode data = root.get("data");
        if (data != null && data.isArray()) {
            T[] items = objectMapper.readValue(data.toString(), arrayClass);
            return items != null ? Arrays.asList(items) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public VacacionesDTO obtenerVacaciones(Integer empleadoId) {
        if (empleadoId == null) {
            return new VacacionesDTO();
        }
        if (skipSaldoVacaciones) {
            // Configurado para evitar golpear el endpoint restringido de saldo.
            return new VacacionesDTO();
        }
        long now = System.currentTimeMillis();
        CacheEntry<VacacionesDTO> cached = cacheVacacionesPorEmpleadoId.get(empleadoId);
        if (cached != null && (now - cached.timestampMs) < DETAILS_CACHE_TTL_MS) {
            return cached.value;
        }
        try {
            String url = "/persona/" + empleadoId + "/saldo_vacaciones/";
            String json = talanaOkHttpService.getFromTalana(url);
            VacacionesDTO value = objectMapper.readValue(json, VacacionesDTO.class);
            cacheVacacionesPorEmpleadoId.put(empleadoId, new CacheEntry<>(value, now));
            return value;
        } catch (IOException e) {
            CacheEntry<VacacionesDTO> cachedAgain = cacheVacacionesPorEmpleadoId.get(empleadoId);
            if (cachedAgain != null && (now - cachedAgain.timestampMs) < DETAILS_CACHE_TTL_MS) {
                return cachedAgain.value;
            }
            throw asUpstreamException(e);
        }
    }

    private static TalanaUpstreamException asUpstreamException(IOException e) {
        int status = extractHttpStatus(e);
        if (status <= 0) {
            status = 502;
        }
        return new TalanaUpstreamException(status, e.getMessage());
    }

    private static int extractHttpStatus(IOException e) {
        if (e == null) return -1;
        String msg = e.getMessage();
        if (msg == null) return -1;
        // Expected format: "HTTP 401 calling ..."
        if (!msg.startsWith("HTTP ")) return -1;
        int space = msg.indexOf(' ', 5);
        if (space < 0) return -1;
        String code = msg.substring(5, space).trim();
        try {
            return Integer.parseInt(code);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public List<LicenciaDTO> obtenerLicencias(Integer empleadoId) {
        if (empleadoId == null) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        CacheEntry<List<LicenciaDTO>> cached = cacheLicenciasPorEmpleadoId.get(empleadoId);
        if (cached != null && (now - cached.timestampMs) < DETAILS_CACHE_TTL_MS) {
            return cached.value;
        }
        try {
            String url = "/ausencia/?empleado=" + empleadoId + "&tipoAusencia=licencia";
            String json = talanaOkHttpService.getFromTalana(url);
            LicenciaDTO[] licencias = objectMapper.readValue(json, LicenciaDTO[].class);
            List<LicenciaDTO> value = Arrays.asList(licencias);
            cacheLicenciasPorEmpleadoId.put(empleadoId, new CacheEntry<>(value, now));
            return value;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Vacaciones detalladas (fechas de ausencias por vacaciones), diferentes del saldo.
     * Usa el mismo esquema de "ausencia" de Talana.
     */
    public List<LicenciaDTO> obtenerVacacionesDetalle(Integer empleadoId) {
        if (empleadoId == null) {
            return Collections.emptyList();
        }
        try {
            String url = "/ausencia/?empleado=" + empleadoId + "&tipoAusencia=vacaciones";
            String json = talanaOkHttpService.getFromTalana(url);
            LicenciaDTO[] vacaciones = objectMapper.readValue(json, LicenciaDTO[].class);
            return Arrays.asList(vacaciones);
        } catch (IOException e) {
            // Si no hay permiso o falla, devolvemos lista vacía; el front puede mostrar mensaje.
            return Collections.emptyList();
        }
    }

    /**
     * Vacaciones resumidas según endpoint de Talana (vacations-resumed/vacaciones-resumen).
     * Devuelve lista para tolerar que el upstream pueda responder objeto único o arreglo.
     */
    public List<VacacionesResumenDTO> obtenerVacacionesResumen(Integer empleadoId) {
        if (empleadoId == null) return Collections.emptyList();
        long now = System.currentTimeMillis();
        CacheEntry<List<VacacionesResumenDTO>> cached = cacheVacacionesResumenPorEmpleadoId.get(empleadoId);
        if (cached != null && (now - cached.timestampMs) < DETAILS_CACHE_TTL_MS) {
            return cached.value;
        }

        List<String> paths = Arrays.asList(
                "/vacations-resumed/?empleado=" + empleadoId,
                "/vacations-resumed?empleado=" + empleadoId,
                "/vacaciones-resumen/?empleado=" + empleadoId,
                "/vacaciones-resumen?empleado=" + empleadoId
        );
        for (String path : paths) {
            try {
                String json = talanaOkHttpService.getFromTalana(path);
                List<VacacionesResumenDTO> list = parseVacacionesResumen(json);
                if (!list.isEmpty()) {
                    cacheVacacionesResumenPorEmpleadoId.put(empleadoId, new CacheEntry<>(list, now));
                    return list;
                }
            } catch (IOException ignored) {
                // intentar siguiente variante
            }
        }
        return Collections.emptyList();
    }

    private List<VacacionesResumenDTO> parseVacacionesResumen(String json) throws IOException {
        if (json == null || json.isBlank()) return Collections.emptyList();
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            VacacionesResumenDTO[] arr = objectMapper.readValue(trimmed, VacacionesResumenDTO[].class);
            return arr != null ? Arrays.asList(arr) : Collections.emptyList();
        }
        JsonNode root = objectMapper.readTree(trimmed);
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            VacacionesResumenDTO[] arr = objectMapper.readValue(results.toString(), VacacionesResumenDTO[].class);
            return arr != null ? Arrays.asList(arr) : Collections.emptyList();
        }
        JsonNode data = root.get("data");
        if (data != null && data.isArray()) {
            VacacionesResumenDTO[] arr = objectMapper.readValue(data.toString(), VacacionesResumenDTO[].class);
            return arr != null ? Arrays.asList(arr) : Collections.emptyList();
        }
        // objeto único
        VacacionesResumenDTO single = objectMapper.readValue(root.toString(), VacacionesResumenDTO.class);
        return single != null ? Collections.singletonList(single) : Collections.emptyList();
    }

    /**
     * Lista de empleados que están de vacaciones en un rango dado (por defecto, hoy).
     * Usa /ausencia/?tipoAusencia=vacaciones y cruza con empleados básicos para enriquecer.
     */
    public List<VacacionesResumenDTO> listarVacacionesActuales(String desde, String hasta, Boolean soloAprobadas) {
        java.time.LocalDate from = null;
        java.time.LocalDate to = null;
        try { if (desde != null && !desde.isBlank()) from = java.time.LocalDate.parse(desde.trim()); } catch (Exception ignored) {}
        try { if (hasta != null && !hasta.isBlank()) to = java.time.LocalDate.parse(hasta.trim()); } catch (Exception ignored) {}
        if (from == null && to == null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            from = today;
            to = today;
        }
        final java.time.LocalDate fromF = from != null ? from : java.time.LocalDate.MIN;
        final java.time.LocalDate toF = to != null ? to : java.time.LocalDate.MAX;
        final boolean onlyApproved = soloAprobadas == null || Boolean.TRUE.equals(soloAprobadas);

        List<String> paths = Arrays.asList(
                    "/ausencia/?tipoAusencia=vacaciones",
                    "/ausencia/?tipoAusencia=vacation",
                    "/ausencias/?tipoAusencia=vacaciones",
                    "/ausencias/?tipoAusencia=vacation",
                    "/ausencia/?tipo=vacaciones",
                    "/ausencias/?tipo=vacaciones"
            );
        List<LicenciaDTO> vacaciones = new ArrayList<>();
        for (String p : paths) {
            try {
                String json = talanaOkHttpService.getFromTalana(p);
                List<LicenciaDTO> chunk = parseList(json, LicenciaDTO[].class);
                if (chunk != null && !chunk.isEmpty()) {
                    vacaciones.addAll(chunk);
                }
            } catch (IOException ignored) {
                // probar siguiente variante
            }
        }
        if (vacaciones.isEmpty()) vacaciones = Collections.emptyList();

            // Mapa empleadoId -> basico
            List<EmpleadoSimpleDTO> basicos = obtenerEmpleadosBasicos();
            Map<Integer, EmpleadoSimpleDTO> byId = basicos.stream()
                    .filter(e -> e.getId() != null)
                    .collect(Collectors.toMap(EmpleadoSimpleDTO::getId, e -> e, (a, b) -> a));

        java.util.List<VacacionesResumenDTO> result = new java.util.ArrayList<>();
        for (LicenciaDTO v : vacaciones) {
            if (v == null || v.getFechaDesde() == null || v.getFechaHasta() == null) continue;
            if (onlyApproved && (v.getAprobada() != null && !v.getAprobada())) continue;
            boolean overlaps = !v.getFechaHasta().isBefore(fromF) && !v.getFechaDesde().isAfter(toF);
            if (!overlaps) continue;

            EmpleadoSimpleDTO base = v.getEmpleado() != null ? byId.get(v.getEmpleado()) : null;
            EmpleadoResumenDTO emp = new EmpleadoResumenDTO();
            if (base != null) {
                emp.setId(base.getId());
                emp.setRut(base.getRut());
                emp.setNombre(base.getNombre());
                emp.setApellidoPaterno(base.getApellidoPaterno());
                emp.setApellidoMaterno(base.getApellidoMaterno());
            } else if (v.getEmpleado() != null) {
                emp.setId(v.getEmpleado());
            }

            VacacionesResumenDTO dto = new VacacionesResumenDTO();
            dto.setEmpleado(emp);
            dto.setVacacionesDesde(v.getFechaDesde());
            dto.setVacacionesHasta(v.getFechaHasta());
            dto.setNumeroDias(v.getNumeroDias());
            dto.setMediosDias(v.getMediosDias());
            dto.setId(v.getId());
            dto.setTipoVacaciones("vacaciones");
            // retorno y fechaAprobacion pueden no estar en ausencia; los dejamos nulos
            result.add(dto);
        }
        return result;
    }
}
