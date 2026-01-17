package com.microservice_employee.controller;

import com.microservice_employee.service.TalanaOkHttpService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@Profile("talana")
@RequestMapping("/api/talana/personas")
public class TalanaPersonaController {

    private final TalanaOkHttpService talana;

    public TalanaPersonaController(TalanaOkHttpService talana) {
        this.talana = talana;
    }

    /**
     * Proxy simple a /es/api/persona-paginado/ (con fallback a /personas-paginadas/).
     * Útil para validar búsqueda/filtros que soporte tu cuenta Talana.
     */
    @GetMapping("/paginado")
    public ResponseEntity<String> listarPaginado(@RequestParam Map<String, String> params) throws IOException {
        Map<String, String> qp = new HashMap<>(params);
        qp.putIfAbsent("page", "1");
        qp.putIfAbsent("page_size", "50");
        return ResponseEntity.ok(talana.getPersonasPaginadas(qp));
    }

    /**
     * Proxy simple a /es/api/persona/{id}/
     */
    @GetMapping("/{personaId}")
    public ResponseEntity<String> obtenerDetalle(@PathVariable int personaId) throws IOException {
        return ResponseEntity.ok(talana.getPersonaDetalle(personaId));
    }
}
