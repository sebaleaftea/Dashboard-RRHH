package com.microservice_employee.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VacacionesResumenDTO {
    private EmpleadoResumenDTO empleado;

    private LocalDate vacacionesDesde;
    private Integer numeroDias;
    private Boolean mediosDias;
    private LocalDate vacacionesHasta;
    private LocalDate vacacionesRetorno;
    private OffsetDateTime fechaAprobacion;

    private Integer id;
    private OffsetDateTime fechaCreacion;
    private String tipoVacaciones;
}
