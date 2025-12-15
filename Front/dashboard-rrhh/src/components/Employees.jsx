import React, { useState } from 'react';

export default function EmployeesList({ employees, onEdit, onDelete }) {
  const [search, setSearch] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Filtro por nombre, cargo, email o centro de costo
  const filtered = employees.filter(emp =>
    (emp.name && emp.name.toLowerCase().includes(search.toLowerCase())) ||
    (emp.position && emp.position.toLowerCase().includes(search.toLowerCase())) ||
    (emp.email && emp.email.toLowerCase().includes(search.toLowerCase())) ||
    (emp.centroCosto?.nombre && emp.centroCosto.nombre.toLowerCase().includes(search.toLowerCase()))
  );

  // Paginación
  const totalPages = Math.ceil(filtered.length / pageSize);
  const paginated = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const handlePageChange = (page) => setCurrentPage(page);

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <input
          type="text"
          className="form-control w-25"
          placeholder="Buscar empleado..."
          value={search}
          onChange={e => {
            setSearch(e.target.value);
            setCurrentPage(1);
          }}
        />
        <span>
          Mostrando {paginated.length} de {filtered.length} empleados
        </span>
      </div>
      <div className="table-responsive">
        <table className="table table-hover align-middle">
          <thead className="table-light">
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Cargo</th>
              <th>Email</th>
              <th>Centro de Costo</th>
              <th>Vacaciones</th>
              <th>Licencias</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {paginated.length === 0 ? (
              <tr>
                <td colSpan={8} className="text-center">No hay empleados que coincidan con la búsqueda.</td>
              </tr>
            ) : (
              paginated.map((emp) => (
                <tr key={emp.id}>
                  <td>#{emp.id}</td>
                  <td className="fw-bold">{emp.name}</td>
                  <td>{emp.position || 'Sin cargo'}</td>
                  <td>{emp.email}</td>
                  <td>{emp.centroCosto?.nombre || 'Sin centro de costo'}</td>
                  <td>
                    {emp.vacations && emp.vacations.length > 0
                      ? emp.vacations.map((v) => (
                          <div key={v.id}>
                            {v.startDate} - {v.endDate}
                          </div>
                        ))
                      : 'Sin vacaciones'}
                  </td>
                  <td>
                    {emp.licenses && emp.licenses.length > 0
                      ? emp.licenses.map((l) => (
                          <div key={l.id}>
                            {l.startDate} - {l.endDate}
                          </div>
                        ))
                      : 'Sin licencias'}
                  </td>
                  <td>
                    <button className="btn btn-sm btn-primary me-2" onClick={() => onEdit(emp)}>
                      Editar
                    </button>
                    <button className="btn btn-sm btn-danger" onClick={() => onDelete(emp.id)}>
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      {/* Paginación */}
      {totalPages > 1 && (
        <nav>
          <ul className="pagination justify-content-center">
            {[...Array(totalPages)].map((_, idx) => (
              <li key={idx} className={`page-item${currentPage === idx + 1 ? ' active' : ''}`}>
                <button className="page-link" onClick={() => handlePageChange(idx + 1)}>
                  {idx + 1}
                </button>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </div>
  );
}