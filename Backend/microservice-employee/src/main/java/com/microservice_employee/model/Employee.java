package com.microservice_employee.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name="employees")
@AllArgsConstructor
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String rut;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private String lastname;

    @Column(nullable=false)
    private String cargo;

    @Column(nullable=false)
    private Integer cod_centro_costo;

    @Column(nullable=false)
    private String nombre_centro_costo;

    @Column(nullable=false)
    private String fecha_ingreso;

    @Column(nullable=false)
    private String sucursal;

    @Column(nullable=false)
    private String jefe;

     @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vacation> vacations;

    // Relación con License
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<License> licenses;

    @ManyToOne
    @JoinColumn(name = "centro_costo_codigo", referencedColumnName = "codigo")
    private CentroCosto centroCosto;
}
