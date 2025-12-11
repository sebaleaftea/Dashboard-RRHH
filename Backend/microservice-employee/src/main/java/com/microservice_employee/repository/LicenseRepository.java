package com.microservice_employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.microservice_employee.model.License;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {

}
