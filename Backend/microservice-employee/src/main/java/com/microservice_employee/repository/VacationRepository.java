package com.microservice_employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.microservice_employee.model.Vacation;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

}
