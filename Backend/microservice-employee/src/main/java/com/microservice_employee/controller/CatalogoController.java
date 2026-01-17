package com.microservice_employee.controller;

import com.microservice_employee.dto.CentroCostoDTO;
import com.microservice_employee.dto.JobTitleDTO;
import com.microservice_employee.dto.SucursalDTO;
import com.microservice_employee.service.TalanaService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("talana")
@RequestMapping("/api")
public class CatalogoController {

    private final TalanaService talanaService;

    public CatalogoController(TalanaService talanaService) {
        this.talanaService = talanaService;
    }

    @GetMapping("/centros-costo")
    public ResponseEntity<List<CentroCostoDTO>> obtenerCentrosCosto(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return ResponseEntity.ok(talanaService.obtenerCentroCostos(limit, offset));
    }

    @GetMapping("/sucursales")
    public ResponseEntity<List<SucursalDTO>> obtenerSucursales(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return ResponseEntity.ok(talanaService.obtenerSucursales(limit, offset));
    }

    @GetMapping("/job-titles")
    public ResponseEntity<List<JobTitleDTO>> obtenerJobTitles(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return ResponseEntity.ok(talanaService.obtenerJobTitles(limit, offset));
    }
}
