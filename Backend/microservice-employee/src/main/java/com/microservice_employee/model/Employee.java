package com.microservice_employee.model;

import java.time.LocalDate;
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
    private String sexo;

    @Column(nullable=false)
    private boolean discapacidad;

    @Column(nullable=false)
    private LocalDate fecha_nacimiento;

    @Column(nullable=false)
    private String cargo;

    @Column(nullable=false)
    private String fecha_ingreso;

    @Column(nullable=false)
    private String sucursal;

    @Column(nullable=false)
    private String jefe;
    
    @ManyToOne
    @JoinColumn(name = "centro_costo_codigo", referencedColumnName = "codigo")
    private CentroCosto centroCosto;
    
     @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vacation> vacations;

    // Relación con License
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<License> licenses;

}
