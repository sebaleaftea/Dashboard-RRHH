import React, { useEffect, useState } from 'react';
import { EmployeesList } from '../components/Employees';

export default function Empleados() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchEmployees = async () => {
      setLoading(true);
      try {
        const res = await fetch('http://localhost:8082/api/employee/all');
        if (!res.ok) throw new Error('Respuesta no OK');
        const data = await res.json();
        setEmployees(data);
      } catch (err) {
        console.error('Error cargando empleados:', err);
        setEmployees([
          { id: 1, name: 'Juan Perez', position: 'Backend Dev', email: 'juan@test.com' },
          { id: 2, name: 'Maria Lopez', position: 'Frontend Dev', email: 'maria@test.com' },
        ]);
      } finally {
        setLoading(false);
      }
    };

    fetchEmployees();
  }, []);

  return (
    <>
      <h2 className="mb-4">Gestión de Empleados</h2>
      {loading ? (
        <div className="text-center p-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Cargando...</span>
          </div>
        </div>
      ) : (
        <EmployeesList employees={employees} />
      )}
    </>
  );
}