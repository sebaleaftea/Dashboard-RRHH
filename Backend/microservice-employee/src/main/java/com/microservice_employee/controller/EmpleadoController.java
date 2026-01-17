package com.microservice_employee.controller;

import com.microservice_employee.dto.EmpleadoDTO;
import com.microservice_employee.dto.EmpleadoSimpleDTO;
import com.microservice_employee.dto.ContratoDTO;
import com.microservice_employee.dto.CentroCostoDTO;
import com.microservice_employee.dto.SucursalDTO;
import com.microservice_employee.dto.VacacionesDTO;
import com.microservice_employee.dto.VacacionesResumenDTO;
import com.microservice_employee.dto.LicenciaDTO;
import com.microservice_employee.service.TalanaService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Collections;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;

@RestController
@Profile("talana")
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final TalanaService talanaService;

    public EmpleadoController(TalanaService talanaService) {
        this.talanaService = talanaService;
    }

    @GetMapping
    public ResponseEntity<List<EmpleadoDTO>> obtenerEmpleados() {
        List<EmpleadoDTO> empleados = talanaService.obtenerEmpleadosCompletos();
        return ResponseEntity.ok(empleados);
    }

    @GetMapping(params = {"limit", "offset"})
    public ResponseEntity<List<EmpleadoDTO>> obtenerEmpleadosPaginados(
            @RequestParam int limit,
            @RequestParam int offset
    ) {
        // Por defecto devolvemos datos básicos (rápido). Para enriquecido usar /enriquecidos.
        List<EmpleadoSimpleDTO> basicos = talanaService.obtenerEmpleadosBasicos(limit, offset);
        List<EmpleadoDTO> page = basicos.stream().map(b -> {
            EmpleadoDTO dto = new EmpleadoDTO();
            BeanUtils.copyProperties(b, dto);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(page);
    }

    @GetMapping(value = "/enriquecidos", params = {"limit", "offset"})
    public ResponseEntity<List<EmpleadoDTO>> obtenerEmpleadosEnriquecidosPaginados(
            @RequestParam int limit,
            @RequestParam int offset
    ) {
        return ResponseEntity.ok(talanaService.obtenerEmpleadosCompletos(limit, offset));
    }

    @GetMapping("/basicos")
    public ResponseEntity<List<EmpleadoSimpleDTO>> obtenerEmpleadosBasicos() {
        return ResponseEntity.ok(talanaService.obtenerEmpleadosBasicos());
    }

    @GetMapping(value = "/basicos", params = {"limit", "offset"})
    public ResponseEntity<List<EmpleadoSimpleDTO>> obtenerEmpleadosBasicosPaginados(
            @RequestParam int limit,
            @RequestParam int offset
    ) {
        return ResponseEntity.ok(talanaService.obtenerEmpleadosBasicos(limit, offset));
    }

    @GetMapping("/rut/{rut}/contrato")
    public ResponseEntity<ContratoDTO> obtenerContratoPorRut(@PathVariable String rut) {
        ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
        return contrato != null ? ResponseEntity.ok(contrato) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{empleadoId}/contrato")
    public ResponseEntity<ContratoDTO> obtenerContratoPorEmpleadoId(@PathVariable Integer empleadoId) {
        ContratoDTO contrato = talanaService.obtenerContratoPorEmpleadoId(empleadoId);
        return contrato != null ? ResponseEntity.ok(contrato) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{rut}/centro-costo")
    public ResponseEntity<CentroCostoDTO> obtenerCentroCostoPorRut(@PathVariable String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        if (empleado == null || empleado.getId() == null) {
            return ResponseEntity.notFound().build();
        }
        ContratoDTO contrato = talanaService.obtenerContratoPorEmpleadoId(empleado.getId());
        if (contrato == null || contrato.getCentroCosto() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(contrato.getCentroCosto());
    }

    @GetMapping("/{rut}/sucursal")
    public ResponseEntity<SucursalDTO> obtenerSucursalPorRut(@PathVariable String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        if (empleado == null || empleado.getId() == null) {
            return ResponseEntity.notFound().build();
        }
        ContratoDTO contrato = talanaService.obtenerContratoPorEmpleadoId(empleado.getId());
        if (contrato == null || contrato.getSucursal() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(contrato.getSucursal());
    }

    @GetMapping("/{empleadoId}/vacaciones")
    public ResponseEntity<VacacionesDTO> obtenerVacaciones(@PathVariable Integer empleadoId) {
        return ResponseEntity.ok(talanaService.obtenerVacaciones(empleadoId));
    }

    @GetMapping("/{empleadoId}/licencias")
    public ResponseEntity<List<LicenciaDTO>> obtenerLicencias(@PathVariable Integer empleadoId) {
        List<LicenciaDTO> licencias = talanaService.obtenerLicencias(empleadoId);
        return ResponseEntity.ok(licencias != null ? licencias : Collections.emptyList());
    }

    @GetMapping("/{empleadoId}/detalle")
    public ResponseEntity<EmpleadoDTO> obtenerDetallePersona(@PathVariable Integer empleadoId) {
        EmpleadoDTO detalle = talanaService.obtenerDetallePersona(empleadoId);
        return detalle != null ? ResponseEntity.ok(detalle) : ResponseEntity.notFound().build();
    }

    /**
     * Alternativa por RUT para evitar el error de conversión cuando se pasa "20834595-8" como empleadoId.
     * Busca el empleado en la lista básica, toma su id y retorna el detalle enriquecido.
     */
    @GetMapping("/rut/{rut}/detalle")
    public ResponseEntity<EmpleadoDTO> obtenerDetallePorRut(@PathVariable String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        Integer empleadoId = empleado != null ? empleado.getId() : null;
        if (empleadoId == null) {
            // Fallback: resolver por contrato activo
            ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
            if (contrato != null && contrato.getEmpleado() != null) {
                empleadoId = contrato.getEmpleado().intValue();
            }
        }
        if (empleadoId == null) {
            return ResponseEntity.notFound().build();
        }
        EmpleadoDTO detalle = talanaService.obtenerDetallePersona(empleadoId);
        return detalle != null ? ResponseEntity.ok(detalle) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{rut}")
    public ResponseEntity<EmpleadoDTO> obtenerEmpleadoPorRut(@PathVariable String rut) {
        return talanaService.obtenerEmpleadosCompletos().stream()
            .filter(e -> e.getRut().equals(rut))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Variantes por query param (evitan 400 cuando se pega RUT en endpoint de ID)
    @GetMapping(value = "/detalle", params = {"rut"})
    public ResponseEntity<EmpleadoDTO> obtenerDetallePorRutQuery(@RequestParam String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        Integer empleadoId = empleado != null ? empleado.getId() : null;
        if (empleadoId == null) {
            ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
            if (contrato != null && contrato.getEmpleado() != null) {
                empleadoId = contrato.getEmpleado().intValue();
            }
        }
        if (empleadoId == null) {
            return ResponseEntity.notFound().build();
        }
        EmpleadoDTO detalle = talanaService.obtenerDetallePersona(empleadoId);
        return detalle != null ? ResponseEntity.ok(detalle) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/contrato", params = {"rut"})
    public ResponseEntity<ContratoDTO> obtenerContratoPorRutQuery(@RequestParam String rut) {
        ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
        return contrato != null ? ResponseEntity.ok(contrato) : ResponseEntity.notFound().build();
    }

    // Variantes por RUT para vacaciones y licencias (comodidad en front)
    @GetMapping(value = "/vacaciones", params = {"rut"})
    public ResponseEntity<VacacionesDTO> obtenerVacacionesPorRut(@RequestParam String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        Integer empleadoId = empleado != null ? empleado.getId() : null;
        if (empleadoId == null) {
            ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
            if (contrato != null && contrato.getEmpleado() != null) {
                empleadoId = contrato.getEmpleado().intValue();
            }
        }
        if (empleadoId == null) {
            return ResponseEntity.notFound().build();
        }
        VacacionesDTO vacaciones = talanaService.obtenerVacaciones(empleadoId);
        return ResponseEntity.ok(vacaciones);
    }

    @GetMapping(value = "/licencias", params = {"rut"})
    public ResponseEntity<List<LicenciaDTO>> obtenerLicenciasPorRut(@RequestParam String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        Integer empleadoId = empleado != null ? empleado.getId() : null;
        if (empleadoId == null) {
            ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
            if (contrato != null && contrato.getEmpleado() != null) {
                empleadoId = contrato.getEmpleado().intValue();
            }
        }
        if (empleadoId == null) {
            return ResponseEntity.notFound().build();
        }
        List<LicenciaDTO> licencias = talanaService.obtenerLicencias(empleadoId);
        return ResponseEntity.ok(licencias != null ? licencias : Collections.emptyList());
    }

    @GetMapping(value = "/vacaciones-detalle", params = {"rut"})
    public ResponseEntity<List<LicenciaDTO>> obtenerVacacionesDetallePorRut(
            @RequestParam String rut,
            @RequestParam(value = "desde", required = false) String desde,
            @RequestParam(value = "hasta", required = false) String hasta
    ) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        Integer empleadoId = empleado != null ? empleado.getId() : null;
        if (empleadoId == null) {
            ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
            if (contrato != null && contrato.getEmpleado() != null) {
                empleadoId = contrato.getEmpleado().intValue();
            }
        }
        if (empleadoId == null) {
            return ResponseEntity.notFound().build();
        }
        List<LicenciaDTO> vacaciones = talanaService.obtenerVacacionesDetalle(empleadoId);
        List<LicenciaDTO> filtered = filtrarPorRangoFechas(vacaciones, desde, hasta);
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{empleadoId}/vacaciones-detalle")
    public ResponseEntity<List<LicenciaDTO>> obtenerVacacionesDetallePorId(
            @PathVariable Integer empleadoId,
            @RequestParam(value = "desde", required = false) String desde,
            @RequestParam(value = "hasta", required = false) String hasta
    ) {
        List<LicenciaDTO> vacaciones = talanaService.obtenerVacacionesDetalle(empleadoId);
        List<LicenciaDTO> filtered = filtrarPorRangoFechas(vacaciones, desde, hasta);
        return ResponseEntity.ok(filtered);
    }

    // Vacaciones resumen (según Talana vacations-resumed), con variantes por ID y por RUT
    @GetMapping(value = "/vacaciones-resumen", params = {"rut"})
    public ResponseEntity<List<VacacionesResumenDTO>> obtenerVacacionesResumenPorRut(
            @RequestParam String rut,
            @RequestParam(value = "desde", required = false) String desde,
            @RequestParam(value = "hasta", required = false) String hasta
    ) {
        Integer empleadoId = resolverEmpleadoIdPorRut(rut);
        if (empleadoId == null) return ResponseEntity.notFound().build();
        List<VacacionesResumenDTO> list = talanaService.obtenerVacacionesResumen(empleadoId);
        List<VacacionesResumenDTO> filtered = filtrarResumenPorRangoFechas(list, desde, hasta);
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{empleadoId}/vacaciones-resumen")
    public ResponseEntity<List<VacacionesResumenDTO>> obtenerVacacionesResumenPorId(
            @PathVariable Integer empleadoId,
            @RequestParam(value = "desde", required = false) String desde,
            @RequestParam(value = "hasta", required = false) String hasta
    ) {
        List<VacacionesResumenDTO> list = talanaService.obtenerVacacionesResumen(empleadoId);
        List<VacacionesResumenDTO> filtered = filtrarResumenPorRangoFechas(list, desde, hasta);
        return ResponseEntity.ok(filtered);
    }

    // Lista de empleados actualmente de vacaciones (por defecto: hoy). Permite rango opcional y soloAprobadas=true por defecto
    @GetMapping("/vacaciones-actuales")
    public ResponseEntity<List<VacacionesResumenDTO>> listarVacacionesActuales(
            @RequestParam(value = "desde", required = false) String desde,
            @RequestParam(value = "hasta", required = false) String hasta,
            @RequestParam(value = "soloAprobadas", required = false) Boolean soloAprobadas
    ) {
        List<VacacionesResumenDTO> list = talanaService.listarVacacionesActuales(desde, hasta, soloAprobadas);
        return ResponseEntity.ok(list);
    }

    private List<LicenciaDTO> filtrarPorRangoFechas(List<LicenciaDTO> items, String desde, String hasta) {
        if (items == null) return Collections.emptyList();
        java.time.LocalDate from = null;
        java.time.LocalDate to = null;
        try { if (desde != null && !desde.isBlank()) from = java.time.LocalDate.parse(desde.trim()); } catch (Exception ignored) {}
        try { if (hasta != null && !hasta.isBlank()) to = java.time.LocalDate.parse(hasta.trim()); } catch (Exception ignored) {}

        if (from == null && to == null) {
            return items;
        }

        final java.time.LocalDate fromF = from != null ? from : java.time.LocalDate.MIN;
        final java.time.LocalDate toF = to != null ? to : java.time.LocalDate.MAX;

        return items.stream()
                .filter(i -> {
                    if (i == null || i.getFechaDesde() == null || i.getFechaHasta() == null) return false;
                    // Mantener si el período se superpone con [fromF, toF]
                    return !i.getFechaHasta().isBefore(fromF) && !i.getFechaDesde().isAfter(toF);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private List<VacacionesResumenDTO> filtrarResumenPorRangoFechas(List<VacacionesResumenDTO> items, String desde, String hasta) {
        if (items == null) return Collections.emptyList();
        java.time.LocalDate from = null;
        java.time.LocalDate to = null;
        try { if (desde != null && !desde.isBlank()) from = java.time.LocalDate.parse(desde.trim()); } catch (Exception ignored) {}
        try { if (hasta != null && !hasta.isBlank()) to = java.time.LocalDate.parse(hasta.trim()); } catch (Exception ignored) {}
        if (from == null && to == null) return items;
        final java.time.LocalDate fromF = from != null ? from : java.time.LocalDate.MIN;
        final java.time.LocalDate toF = to != null ? to : java.time.LocalDate.MAX;
        return items.stream()
                .filter(i -> {
                    if (i == null || i.getVacacionesDesde() == null || i.getVacacionesHasta() == null) return false;
                    return !i.getVacacionesHasta().isBefore(fromF) && !i.getVacacionesDesde().isAfter(toF);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private Integer resolverEmpleadoIdPorRut(String rut) {
        EmpleadoSimpleDTO empleado = talanaService.obtenerEmpleadosBasicos().stream()
                .filter(e -> e.getRut() != null && e.getRut().equalsIgnoreCase(rut))
                .findFirst()
                .orElse(null);
        Integer empleadoId = empleado != null ? empleado.getId() : null;
        if (empleadoId == null) {
            ContratoDTO contrato = talanaService.obtenerContratoPorRut(rut);
            if (contrato != null && contrato.getEmpleado() != null) {
                empleadoId = contrato.getEmpleado().intValue();
            }
        }
        return empleadoId;
    }
}
