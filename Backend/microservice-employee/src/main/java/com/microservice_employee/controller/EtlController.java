package com.microservice_employee.controller;

import com.microservice_employee.service.EtlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/etl")
public class EtlController {
    private final EtlService etlService;

    public EtlController(EtlService etlService) {
        this.etlService = etlService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(@RequestParam(name = "onlyActives", required = false, defaultValue = "true") boolean onlyActives) {
        Map<String, Object> summary = etlService.runInitialLoad(onlyActives);
        return ResponseEntity.ok(summary);
    }
}
