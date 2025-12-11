package com.microservice_employee.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microservice_employee.model.Employee;
import com.microservice_employee.repository.EmployeeRepository;

@Service
public class EmployeeService {

    @Autowired
    public EmployeeRepository employeeRepository;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee findByName(String name) {
        return employeeRepository.findByName(name).orElse(null);
    }

    public void deleteEmployee(Long id){
        employeeRepository.deleteById(id);
    }

    //Metodo para el update de empleado
    public Employee update(Long id, Employee emp) {
    Employee existing = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
    existing.setName(emp.getName());
    existing.setPosition(emp.getPosition());
    existing.setEmail(emp.getEmail());
    // Agrega aquí otros campos que quieras actualizar
    return employeeRepository.save(existing);
}

}
