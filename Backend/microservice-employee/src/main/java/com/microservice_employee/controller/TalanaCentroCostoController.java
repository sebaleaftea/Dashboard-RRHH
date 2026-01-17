package com.microservice_employee.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.microservice_employee.service.TalanaOkHttpService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@Profile("talana")
@RequestMapping("/api/talana/centros-costo")
public class TalanaCentroCostoController {

    private final TalanaOkHttpService talana;

    public TalanaCentroCostoController(TalanaOkHttpService talana) {
        this.talana = talana;
    }

    /**
     * Proxy a /es/api/centro-costo/ (con fallback a /centroCosto/ vía TalanaOkHttpService).
     */
    @GetMapping
    public ResponseEntity<String> listar(@RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) Integer offset) throws IOException {
        return ResponseEntity.ok(talana.getCentroCostos(limit, offset));
    }

    /**
     * Proxy genérico para crear centro de costo (POST /es/api/centro-costo/).
     */
    @PostMapping
    public ResponseEntity<String> crear(@RequestBody(required = false) JsonNode body) throws IOException {
        if (body == null || body.isNull()) {
            return ResponseEntity.badRequest().body("Missing JSON body");
        }
        // La ruta oficial suele ser /centro-costo/
        try {
            return ResponseEntity.ok(talana.postToTalana("/centro-costo/", body.toString()));
        } catch (IOException ex) {
            // fallback legacy
            return ResponseEntity.ok(talana.postToTalana("/centroCosto/", body.toString()));
        }
    }

    /**
     * Proxy genérico para actualizar centro de costo (PATCH /es/api/centro-costo/{id}/).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable long id, @RequestBody(required = false) JsonNode body) throws IOException {
        if (body == null || body.isNull()) {
            return ResponseEntity.badRequest().body("Missing JSON body");
        }
        try {
            return ResponseEntity.ok(talana.patchToTalana("/centro-costo/" + id + "/", body.toString()));
        } catch (IOException ex) {
            return ResponseEntity.ok(talana.patchToTalana("/centroCosto/" + id + "/", body.toString()));
        }
    }
}
