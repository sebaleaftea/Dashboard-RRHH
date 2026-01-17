package com.microservice_employee.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmpleadoDTO {
    private Integer id;

    private String rut;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String sexo; // "M"/"F" u otros valores del API
    private Boolean tieneDiscapacidad; // true si "discapacidades" no está vacío en detalles
    private String cargo;
    private String codigoCentroCosto;
    private String nombreCentroCosto;
    private String sucursal;
    private EmpleadoSimpleDTO jefe;
    private LocalDate fechaIngreso;
    private VacacionesDTO vacaciones;
    private List<LicenciaDTO> licencias;
}

