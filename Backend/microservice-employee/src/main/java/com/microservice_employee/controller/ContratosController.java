package com.microservice_employee.controller;

import com.microservice_employee.dto.ContratoResumenDTO;
import com.microservice_employee.service.TalanaService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@Profile("talana")
@RequestMapping("/api/contratos")
public class ContratosController {

    private final TalanaService talanaService;

    public ContratosController(TalanaService talanaService) {
        this.talanaService = talanaService;
    }

    /**
     * Lista contratos históricos desde Talana, paginando, y devuelve sólo los campos necesarios para el front.
     * Params opcionales: search_since (yyyyMMdd), search_to (yyyyMMdd), page_size.
     */
    @GetMapping("/historico")
    public ResponseEntity<List<ContratoResumenDTO>> historico(
            @RequestParam(value = "search_since", required = false) String searchSince,
            @RequestParam(value = "search_to", required = false) String searchTo,
            @RequestParam(value = "page_size", required = false) Integer pageSize
    ) {
        List<ContratoResumenDTO> list = talanaService.listarContratosHistorico(searchSince, searchTo, pageSize);
        return ResponseEntity.ok(list);
    }

    /**
     * Lista contratos activos (vigentes). Si no se pasa "page", recorre todas las páginas.
     * Params opcionales: page, page_size.
     */
    @GetMapping("/activos")
    public ResponseEntity<List<ContratoResumenDTO>> activos(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force
    ) {
        List<ContratoResumenDTO> list = talanaService.listarContratosActivos(page, pageSize, force);
        return ResponseEntity.ok(list);
    }

    /**
     * Forzar refresco de la caché de contratos activos y devolver métricas simples.
     * Útil para precalentar manualmente sin esperar al scheduler diario.
     */
    @PostMapping("/activos/refresh")
    public ResponseEntity<Map<String, Object>> refreshActivos() {
        List<ContratoResumenDTO> list = talanaService.listarContratosActivos(null, null, true);
        Map<String, Object> resp = new HashMap<>();
        resp.put("refreshed", true);
        resp.put("count", list != null ? list.size() : 0);
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(resp);
    }
}
