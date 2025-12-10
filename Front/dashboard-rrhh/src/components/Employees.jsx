import React from 'react';

export function EmployeesList({ employees }) {
  if (!employees || employees.length === 0) {
    return <div className="alert alert-info text-center">No hay empleados registrados.</div>;
  }

  return (
    <div className="table-responsive">
      <table className="table table-hover align-middle">
        <thead className="table-light">
          <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Cargo</th>
            <th>Email</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((emp) => (
            <tr key={emp.id}>
              <td>#{emp.id}</td>
              <td className="fw-bold">{emp.name}</td>
              <td>{emp.position || 'Sin cargo'}</td>
              <td>{emp.email}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}