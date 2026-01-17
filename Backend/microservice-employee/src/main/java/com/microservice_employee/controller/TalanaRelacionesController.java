package com.microservice_employee.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microservice_employee.service.TalanaOkHttpService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@Profile("talana")
@RequestMapping("/api/talana/relaciones")
public class TalanaRelacionesController {

    private final TalanaOkHttpService talana;
    private final ObjectMapper mapper = new ObjectMapper();

    public TalanaRelacionesController(TalanaOkHttpService talana) {
        this.talana = talana;
    }

    /**
     * Obtiene todos los contratos (paginados) y filtra por sucursalId.
     * Para evitar cargas grandes, limita por maxPages (default 10).
     */
    @GetMapping("/sucursal/{sucursalId}/contratos")
    public ResponseEntity<String> contratosDeSucursal(@PathVariable long sucursalId,
                                                      @RequestParam(required = false, defaultValue = "10") int maxPages,
                                                      @RequestParam(required = false, defaultValue = "100") int pageSize,
                                                      @RequestParam(required = false, defaultValue = "last") String showVersion,
                                                      @RequestParam(required = false, defaultValue = "actives") String typeStatusSearch) throws IOException {
        List<JsonNode> contratos = fetchContratos(maxPages, pageSize, showVersion, typeStatusSearch);
        ArrayNode out = mapper.createArrayNode();
        for (JsonNode c : contratos) {
            Long sid = readNestedId(c, "sucursal");
            if (sid != null && sid == sucursalId) {
                out.add(c);
            }
        }
        return ResponseEntity.ok(out.toString());
    }

    /**
     * Lista centros de costo utilizados en una sucursal.
     */
    @GetMapping("/sucursal/{sucursalId}/centros-costo")
    public ResponseEntity<String> centrosCostoDeSucursal(@PathVariable long sucursalId,
                                                         @RequestParam(required = false, defaultValue = "10") int maxPages,
                                                         @RequestParam(required = false, defaultValue = "100") int pageSize,
                                                         @RequestParam(required = false, defaultValue = "last") String showVersion,
                                                         @RequestParam(required = false, defaultValue = "actives") String typeStatusSearch) throws IOException {
        List<JsonNode> contratos = fetchContratos(maxPages, pageSize, showVersion, typeStatusSearch);

        Map<Long, JsonNode> centrosById = new LinkedHashMap<>();
        for (JsonNode c : contratos) {
            Long sid = readNestedId(c, "sucursal");
            if (sid == null || sid != sucursalId) continue;

            JsonNode cc = c.get("centroCosto");
            if (cc != null && cc.isObject()) {
                Long ccId = readLong(cc, "id");
                if (ccId != null) {
                    centrosById.putIfAbsent(ccId, cc);
                }
            }
        }

        ArrayNode out = mapper.createArrayNode();
        for (JsonNode v : centrosById.values()) {
            out.add(v);
        }
        return ResponseEntity.ok(out.toString());
    }

    /**
     * Obtiene empleados (IDs) de un centro de costo a través de contratos paginados.
     */
    @GetMapping("/centro-costo/{centroCostoId}/empleados")
    public ResponseEntity<String> empleadosDeCentroCosto(@PathVariable long centroCostoId,
                                                         @RequestParam(required = false, defaultValue = "10") int maxPages,
                                                         @RequestParam(required = false, defaultValue = "100") int pageSize,
                                                         @RequestParam(required = false, defaultValue = "last") String showVersion,
                                                         @RequestParam(required = false, defaultValue = "actives") String typeStatusSearch) throws IOException {
        List<JsonNode> contratos = fetchContratos(maxPages, pageSize, showVersion, typeStatusSearch);

        Set<Long> empleadoIds = new LinkedHashSet<>();
        for (JsonNode c : contratos) {
            Long ccId = readNestedId(c, "centroCosto");
            if (ccId == null || ccId != centroCostoId) continue;

            Long empleadoId = readLong(c, "empleado");
            if (empleadoId != null) {
                empleadoIds.add(empleadoId);
            }
        }

        ArrayNode out = mapper.createArrayNode();
        for (Long id : empleadoIds) {
            out.add(id);
        }
        return ResponseEntity.ok(out.toString());
    }

    private List<JsonNode> fetchContratos(int maxPages, int pageSize, String showVersion, String typeStatusSearch) throws IOException {
        int safeMaxPages = Math.max(1, Math.min(maxPages, 50));
        int safePageSize = Math.max(1, Math.min(pageSize, 100));

        List<JsonNode> out = new ArrayList<>();
        for (int page = 1; page <= safeMaxPages; page++) {
            Map<String, String> qp = new HashMap<>();
            qp.put("page", String.valueOf(page));
            qp.put("page_size", String.valueOf(safePageSize));
            if (showVersion != null && !showVersion.isBlank()) {
                qp.put("show_version", showVersion);
            }
            if (typeStatusSearch != null && !typeStatusSearch.isBlank()) {
                qp.put("type_status_search", typeStatusSearch);
            }

            String json = talana.getContratosPaginados(qp);
            JsonNode root = mapper.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.size() == 0) {
                break;
            }
            for (JsonNode c : results) {
                if (c != null && !c.isNull()) {
                    out.add(c);
                }
            }

            JsonNode next = root.get("next");
            if (next == null || next.isNull() || (next.isTextual() && next.asText().isBlank())) {
                break;
            }
        }
        return out;
    }

    private static Long readNestedId(JsonNode obj, String fieldName) {
        if (obj == null || fieldName == null) return null;
        JsonNode v = obj.get(fieldName);
        if (v == null || v.isNull()) return null;
        if (v.isObject()) {
            return readLong(v, "id");
        }
        if (v.isInt() || v.isLong()) {
            return v.asLong();
        }
        if (v.isTextual()) {
            try {
                return Long.parseLong(v.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long readLong(JsonNode obj, String fieldName) {
        if (obj == null || fieldName == null) return null;
        JsonNode v = obj.get(fieldName);
        if (v == null || v.isNull()) return null;
        if (v.isInt() || v.isLong()) return v.asLong();
        if (v.isTextual()) {
            try {
                return Long.parseLong(v.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
