import { useState, useEffect } from 'react';
import axios from 'axios';
import ModalLista from '../components/ModalLista';
import "../styles/employees.css";


const API_URL = 'http://localhost:8082/api/v1/employees';

export default function Empleados() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    name: '',
    lastname: '',
    position: '',
    email: '',
    contract: ''
  });
  const [editId, setEditId] = useState(null);

  // Estados para el modal de vacaciones/licencias
  const [showModal, setShowModal] = useState(false);
  const [modalTitle, setModalTitle] = useState('');
  const [modalItems, setModalItems] = useState([]);
  const [modalTipo, setModalTipo] = useState('');

  // Estados para búsqueda y paginación
  const [search, setSearch] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  const fetchEmployees = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`${API_URL}/all`);
      console.log('Respuesta backend:', res.data); 

      // Ajusta aquí según la estructura real de la respuesta:
      if (Array.isArray(res.data)) {
        setEmployees(res.data);
      } else if (Array.isArray(res.data.employees)) {
        setEmployees(res.data.employees);
      } else {
        setEmployees([]);
      }
    } catch (err) {
      console.error('Error obteniendo empleados:', err);
      setEmployees([]); 
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  const handleChange = e => {
    setForm({
      ...form,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async e => {
    e.preventDefault();
    try {
      if (editId) {
        await axios.put(`${API_URL}/${editId}`, form);
      } else {
        await axios.post(`${API_URL}/create`, form);
      }
      setForm({
        name: '',
        lastname: '',
        position: '',
        email: '',
        contract: ''
      });
      setEditId(null);
      fetchEmployees();
    } catch (err) {
      console.error('Error guardando empleado:', err);
    }
  };

  const handleEdit = emp => {
    setForm({
      name: emp.name || '',
      lastname: emp.lastname || '',
      position: emp.position || '',
      email: emp.email || '',
      contract: emp.contract || ''
    });
    setEditId(emp.id);
  };

  const handleDelete = async id => {
    try {
      await axios.delete(`${API_URL}/delete/${id}`);
      fetchEmployees();
    } catch (err) {
      console.error('Error eliminando empleado:', err);
    }
  };

  // Handlers para mostrar vacaciones y licencias en el modal
  const handleShowVacaciones = (vacations) => {
    setModalTitle('Vacaciones');
    setModalItems(vacations);
    setModalTipo('vacaciones');
    setShowModal(true);
  };

  const handleShowLicencias = (licenses) => {
    setModalTitle('Licencias');
    setModalItems(licenses);
    setModalTipo('licencias');
    setShowModal(true);
  };

  // --- Búsqueda y paginación ---
  const filtered = Array.isArray(employees) ? employees.filter(emp =>
    (emp.rut && emp.rut.toLowerCase().includes(search.toLowerCase())) ||
    (emp.name && emp.name.toLowerCase().includes(search.toLowerCase())) ||
    (emp.lastname && emp.lastname.toLowerCase().includes(search.toLowerCase())) ||
    (emp.cargo && emp.cargo.toLowerCase().includes(search.toLowerCase())) ||
    (emp.centroCosto?.nombre && emp.centroCosto.nombre.toLowerCase().includes(search.toLowerCase()))
  ) : [];

  const totalPages = Math.ceil(filtered.length / pageSize);
  const paginated = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const handlePageChange = (page) => setCurrentPage(page);

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Empleados</h2>
      <form className="row g-3 mb-4" onSubmit={handleSubmit}>
        <div className="col-md-2">
          <input
            name="name"
            value={form.name}
            onChange={handleChange}
            className="form-control"
            placeholder="Nombre"
            required
          />
        </div>
        <div className="col-md-2">
          <input
            name="lastname"
            value={form.lastname}
            onChange={handleChange}
            className="form-control"
            placeholder="Apellido"
            required
          />
        </div>
        <div className="col-md-2">
          <input
            name="position"
            value={form.position}
            onChange={handleChange}
            className="form-control"
            placeholder="Puesto"
            required
          />
        </div>
        <div className="col-md-3">
          <input
            name="email"
            value={form.email}
            onChange={handleChange}
            className="form-control"
            placeholder="Email"
            required
          />
        </div>
        <div className="col-md-2">
          <input
            name="contract"
            value={form.contract}
            onChange={handleChange}
            className="form-control"
            placeholder="Contrato"
            required
          />
        </div>
        <div className="col-md-1 d-flex align-items-center">
          <button type="submit" className={`btn ${editId ? 'btn-warning' : 'btn-primary'} me-2`}>
            {editId ? 'Actualizar' : 'Crear'}
          </button>
          {editId && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setForm({
                  name: '',
                  lastname: '',
                  position: '',
                  email: '',
                  contract: ''
                });
                setEditId(null);
              }}
            >
              Cancelar
            </button>
          )}
        </div>
      </form>
      {/* Buscador */}
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
      {loading ? (
        <div className="text-center my-4">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Cargando...</span>
          </div>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table table-striped table-bordered align-middle">
            <thead className="table-dark">
              <tr>
                <th>RUT</th>
                <th>Nombre</th>
                <th>Apellido</th>
                <th>Cargo</th>
                <th>Código Centro Costo</th>
                <th>Nombre Centro Costo</th>
                <th>Sucursal</th>
                <th>Jefe</th>
                <th>Fecha Ingreso</th>
                <th>Vacaciones</th>
                <th>Licencias</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {paginated.length === 0 ? (
                <tr>
                  <td colSpan={12} className="text-center">No hay empleados que coincidan con la búsqueda.</td>
                </tr>
              ) : (
                paginated.map(emp => (
                  <tr key={emp.id}>
                    <td>{emp.rut}</td>
                    <td>{emp.name}</td>
                    <td>{emp.lastname}</td>
                    <td>{emp.cargo}</td>
                    <td>{emp.centroCosto?.codigo || ''}</td>
                    <td>{emp.centroCosto?.nombre || ''}</td>
                    <td>{emp.sucursal}</td>
                    <td>{emp.jefe}</td>
                    <td>{emp.fecha_ingreso}</td>
                    <td>
                      <button
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => handleShowVacaciones(emp.vacations)}
                      >
                        Ver Vacaciones
                      </button>
                    </td>
                    <td>
                      <button
                        className="btn btn-sm btn-outline-success"
                        onClick={() => handleShowLicencias(emp.licenses)}
                      >
                        Ver Licencias
                      </button>
                    </td>
                    <td>
                      <button
                        className="btn btn-sm btn-info me-2"
                        onClick={() => handleEdit(emp)}
                      >
                        Editar
                      </button>
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => handleDelete(emp.id)}
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
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
      <ModalLista
        show={showModal}
        onClose={() => setShowModal(false)}
        title={modalTitle}
        items={modalItems}
        tipo={modalTipo}
      />
    </div>
  );
}