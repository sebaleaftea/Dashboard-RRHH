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
@RequestMapping("/api/talana/contratos")
public class TalanaContratoController {

    private final TalanaOkHttpService talana;

    public TalanaContratoController(TalanaOkHttpService talana) {
        this.talana = talana;
    }

    /**
     * Proxy simple a /es/api/contrato-paginado/ para debugging.
     * Params comunes: page, page_size, empleado, rut, type_status_search, show_version,
     * active_on, hired_since, hired_to, terminated_since, terminated_to.
     */
    @GetMapping("/paginado")
    public ResponseEntity<String> listarPaginado(@RequestParam Map<String, String> params) throws IOException {
        Map<String, String> qp = new HashMap<>(params);
        // defaults razonables
        qp.putIfAbsent("page", "1");
        qp.putIfAbsent("page_size", "50");
        qp.putIfAbsent("show_version", "last");

        return ResponseEntity.ok(talana.getContratosPaginados(qp));
    }

    /**
     * Proxy simple a /es/api/contrato/{id}/ para obtener detalle (incluye jefe si está disponible).
     */
    @GetMapping("/{contratoId}")
    public ResponseEntity<String> obtenerDetalle(@PathVariable long contratoId) throws IOException {
        return ResponseEntity.ok(talana.getContratoDetalle(contratoId));
    }

    /**
     * Proxy genérico para crear contrato (POST /es/api/contrato/).
     * Nota: requiere permisos en el token.
     */
    @PostMapping
    public ResponseEntity<String> crear(@RequestBody(required = false) JsonNode body) throws IOException {
        if (body == null || body.isNull()) {
            return ResponseEntity.badRequest().body("Missing JSON body");
        }
        return ResponseEntity.ok(talana.postToTalana("/contrato/", body.toString()));
    }

    /**
     * Proxy genérico para actualizar contrato (PATCH /es/api/contrato/{id}/).
     */
    @PatchMapping("/{contratoId}")
    public ResponseEntity<String> actualizar(@PathVariable long contratoId, @RequestBody(required = false) JsonNode body) throws IOException {
        if (body == null || body.isNull()) {
            return ResponseEntity.badRequest().body("Missing JSON body");
        }
        return ResponseEntity.ok(talana.patchToTalana("/contrato/" + contratoId + "/", body.toString()));
    }
}
