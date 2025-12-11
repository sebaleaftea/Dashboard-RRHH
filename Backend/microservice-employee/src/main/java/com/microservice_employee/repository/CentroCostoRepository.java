package com.microservice_employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.microservice_employee.model.CentroCosto;

public interface CentroCostoRepository extends JpaRepository<CentroCosto, Integer> {

}
