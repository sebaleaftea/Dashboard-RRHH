package com.microservice_employee.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.microservice_employee.service.TalanaOkHttpService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Profile("talana")
@RequestMapping("/api/talana/sucursales")
public class TalanaSucursalController {

    private final TalanaOkHttpService talana;

    public TalanaSucursalController(TalanaOkHttpService talana) {
        this.talana = talana;
    }

    /**
     * Proxy a /es/api/sucursal/
     */
    @GetMapping
    public ResponseEntity<String> listar(@RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) Integer offset) throws IOException {
        return ResponseEntity.ok(talana.getSucursales(limit, offset));
    }

    /**
     * Proxy genérico para crear sucursal (POST /es/api/sucursal/).
     */
    @PostMapping
    public ResponseEntity<String> crear(@RequestBody(required = false) JsonNode body) throws IOException {
        if (body == null || body.isNull()) {
            return ResponseEntity.badRequest().body("Missing JSON body");
        }
        return ResponseEntity.ok(talana.postToTalana("/sucursal/", body.toString()));
    }

    /**
     * Proxy genérico para actualizar sucursal (PATCH /es/api/sucursal/{id}/).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable long id, @RequestBody(required = false) JsonNode body) throws IOException {
        if (body == null || body.isNull()) {
            return ResponseEntity.badRequest().body("Missing JSON body");
        }
        return ResponseEntity.ok(talana.patchToTalana("/sucursal/" + id + "/", body.toString()));
    }
}
