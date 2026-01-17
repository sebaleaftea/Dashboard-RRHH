package com.microservice_employee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobTitleDTO {
    private Integer id;

    // Talana puede exponerlo como "name" o "nombre" dependiendo del entorno
    private String name;
    private String nombre;

    private String codigo;
}
