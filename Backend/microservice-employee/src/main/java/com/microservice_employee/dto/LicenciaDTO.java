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
public class LicenciaDTO {
    private Integer id;
    private Integer empleado;
    private String tipoAusencia;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Integer numeroDias;
    private Boolean aprobada;
    private Boolean mediosDias;
}

