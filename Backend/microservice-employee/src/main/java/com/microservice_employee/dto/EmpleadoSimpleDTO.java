package com.microservice_employee.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmpleadoSimpleDTO {
    private Integer id;
    private String rut;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
}
