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
public class ContratoDTO {
    private Long id;
    private Long empleado;
    private String cargo;
    private CentroCostoDTO centroCosto;
    private SucursalDTO sucursal;
    private LocalDate fechaContratacion;
    private EmpleadoSimpleDTO jefe;

}
