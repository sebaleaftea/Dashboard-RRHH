package com.microservice_employee.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContratoResumenDTO {
    private Long contratoId;
    private Long empleadoId;
    private String rut;
    private String nombre;
    private String apellidoPaterno;

    private String cargo;
    private String centroCostoCodigo;
    private String centroCostoNombre;
    private String sucursalNombre;
    private String jefeNombre;
    private LocalDate fechaContratacion;
}
