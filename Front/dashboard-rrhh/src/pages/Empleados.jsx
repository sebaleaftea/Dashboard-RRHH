import { useState, useEffect } from 'react';
import axios from 'axios';
import ModalLista from '../components/ModalLista';
import * as XLSX from 'xlsx';
import "../styles/employees.css";
import { API_URLS } from '../config/api';


const API_URL = `${API_URLS.EMPLOYEE_SERVICE}/api/db/empleados`;
const API_CONTRATOS_ACTIVOS_URL = `${API_URLS.EMPLOYEE_SERVICE}/api/db/contratos/activos`;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export default function Empleados() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [vacacionesVigentes, setVacacionesVigentes] = useState({});
  const [licenciasVigentes, setLicenciasVigentes] = useState({});
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

  const formatJefe = (jefe) => {
    if (!jefe) return '';
    if (typeof jefe === 'string') return jefe;
    const fullName = `${jefe.nombre || ''} ${jefe.apellidoPaterno || ''}`.trim();
    return fullName || jefe.rut || '';
  };

  // Exportar a Excel
  const exportToExcel = () => {
    const data = filtered.map(emp => ({
      RUT: emp.rut,
      Nombre: emp.nombre,
      Apellido: emp.apellidoPaterno,
      Cargo: emp.cargo,
      CodigoCentroCosto: emp.codigoCentroCosto || '',
      NombreCentroCosto: emp.nombreCentroCosto || '',
      Sucursal: emp.sucursal,
      Jefe: formatJefe(emp.jefe),
      FechaIngreso: emp.fechaIngreso
    }));
    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Empleados');
    XLSX.writeFile(wb, 'Empleados.xlsx');
  };
  

  const fetchEmployees = async () => {
    setLoading(true);
    try {
      const res = await axios.get(API_CONTRATOS_ACTIVOS_URL);
      console.log('Contratos activos (resumen):', res.data);
      if (Array.isArray(res.data)) {
        // Mapear a la forma esperada por la tabla
        const mapped = res.data.map(item => ({
          id: item.empleadoId,
          rut: item.rut,
          nombre: item.nombre,
          apellidoPaterno: item.apellidoPaterno,
          cargo: item.cargo,
          codigoCentroCosto: item.centroCostoCodigo,
          nombreCentroCosto: item.centroCostoNombre,
          sucursal: item.sucursalNombre,
          jefe: item.jefeNombre || '', // puede venir vacío
          fechaIngreso: item.fechaContratacion,
        }));
        setEmployees(mapped);
        // Cargar vacaciones y licencias vigentes
        fetchVacacionesYLicenciasVigentes(mapped);
      } else {
        setEmployees([]);
      }
    } catch (err) {
      console.error('Error obteniendo empleados:', err);
      setEmployees([]);
    }
    setLoading(false);
  };

  const fetchVacacionesYLicenciasVigentes = async (empleadosList) => {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    
    const vacacionesMap = {};
    const licenciasMap = {};
    
    // Procesar en lotes para no sobrecargar
    for (const emp of empleadosList) {
      try {
        // Obtener vacaciones del empleado
        const vacRes = await axios.get(`${API_URL}/${emp.id}/vacaciones`);
        if (Array.isArray(vacRes.data)) {
          // Filtrar las vigentes (fecha actual está entre desde y hasta)
          const vigente = vacRes.data.find(v => {
            const desde = new Date(v.desde);
            const hasta = new Date(v.hasta || v.retorno);
            desde.setHours(0, 0, 0, 0);
            hasta.setHours(0, 0, 0, 0);
            return hoy >= desde && hoy <= hasta;
          });
          if (vigente) {
            vacacionesMap[emp.id] = vigente;
          }
        }
        
        // Obtener licencias del empleado
        const licRes = await axios.get(`${API_URL}/${emp.id}/licencias`);
        if (Array.isArray(licRes.data)) {
          // Filtrar las vigentes
          const vigente = licRes.data.find(l => {
            const desde = new Date(l.desde);
            const hasta = new Date(l.hasta);
            desde.setHours(0, 0, 0, 0);
            hasta.setHours(0, 0, 0, 0);
            return hoy >= desde && hoy <= hasta;
          });
          if (vigente) {
            licenciasMap[emp.id] = vigente;
          }
        }
      } catch (err) {
        console.warn(`Error obteniendo vacaciones/licencias para empleado ${emp.id}:`, err);
      }
    }
    
    setVacacionesVigentes(vacacionesMap);
    setLicenciasVigentes(licenciasMap);
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
  const handleShowVacaciones = async (empleadoId) => {
    setModalTitle('Vacaciones');
    setModalTipo('vacaciones');
    try {
      const res = await axios.get(`${API_URL}/${empleadoId}/vacaciones`);
      setModalItems(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Error obteniendo vacaciones:', err);
      setModalItems([]);
    }
    setShowModal(true);
  };

  const handleShowLicencias = async (empleadoId) => {
    setModalTitle('Licencias');
    setModalTipo('licencias');
    try {
      const res = await axios.get(`${API_URL}/${empleadoId}/licencias`);
      setModalItems(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Error obteniendo licencias:', err);
      setModalItems([]);
    }
    setShowModal(true);
  };

  const fetchDetalleForEmployee = async (emp) => {
    if (!emp?.id) return;
    // si ya está lleno, no volver a pedir
    if (emp.cargo || emp.codigoCentroCosto || emp.fechaIngreso || emp.jefe || emp.sucursal) return;

    try {
      const res = await axios.get(`${API_URL}/${emp.id}/detalle`);
      const detalle = res.data;
      setEmployees((prev) => prev.map((e) => {
        if (e.id !== emp.id) return e;
        return {
          ...e,
          cargo: detalle?.cargo || e.cargo,
          codigoCentroCosto: detalle?.codigoCentroCosto || e.codigoCentroCosto,
          nombreCentroCosto: detalle?.nombreCentroCosto || e.nombreCentroCosto,
          sucursal: detalle?.sucursal || e.sucursal,
          fechaIngreso: detalle?.fechaIngreso || e.fechaIngreso,
          jefe: detalle?.jefe || e.jefe,
        };
      }));
    } catch (err) {
      console.warn('Error obteniendo detalle persona:', emp?.id, err);
    }
  };

  // --- Búsqueda y paginación ---
  const filtered = Array.isArray(employees) ? employees.filter(emp =>
    (emp.rut && emp.rut.toLowerCase().includes(search.toLowerCase())) ||
    (emp.nombre && emp.nombre.toLowerCase().includes(search.toLowerCase())) ||
    (emp.apellidoPaterno && emp.apellidoPaterno.toLowerCase().includes(search.toLowerCase())) ||
    (emp.cargo && emp.cargo.toLowerCase().includes(search.toLowerCase())) ||
    (emp.nombreCentroCosto && emp.nombreCentroCosto.toLowerCase().includes(search.toLowerCase()))
  ) : [];

  const totalPages = Math.ceil(filtered.length / pageSize);
  const paginated = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const handlePageChange = (page) => setCurrentPage(page);

  // --- Paginación dinámica ---
  const maxPageButtons = 10;
  let startPage = Math.max(1, currentPage - Math.floor(maxPageButtons / 2));
  let endPage = startPage + maxPageButtons - 1;
  if (endPage > totalPages) {
    endPage = totalPages;
    startPage = Math.max(1, endPage - maxPageButtons + 1);
  }
  const pageNumbers = [];
  for (let i = startPage; i <= endPage; i++) {
    pageNumbers.push(i);
  }

  // Eliminado enriquecimiento por fila para evitar N+1 y demoras.

  return (
    <div className="container mt-4" style={{ maxWidth: '1200px' }}>
      <div className="d-flex justify-content-end mb-2">
        <button className="btn btn-success me-2" onClick={exportToExcel}>
          Exportar a Excel
        </button>
      </div>
      <h2 className="mb-4">Empleados</h2>
      <form className="row g-3 mb-4" onSubmit={handleSubmit}>
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
        <>
          {loadingDetails && (
            <div className="text-end text-muted mb-2">
              Cargando detalle...
            </div>
          )}
          <div className="table-responsive">
            <table className="table table-striped table-bordered align-middle" id="empleados-table">
              <thead className="table-dark">
                <tr>
                  <th>RUT</th>
                  <th>Nombre</th>
                  <th>Apellido Paterno</th>
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
                      <td>{emp.nombre}</td>
                      <td>{emp.apellidoPaterno}</td>
                      <td>{emp.cargo || ''}</td>
                      <td>{emp.codigoCentroCosto || ''}</td>
                      <td>{emp.nombreCentroCosto || ''}</td>
                      <td>{emp.sucursal || ''}</td>
                      <td>{formatJefe(emp.jefe)}</td>
                      <td>{emp.fechaIngreso || ''}</td>
                      <td>
                        <button
                          className={`btn btn-sm ${vacacionesVigentes[emp.id] ? 'btn-primary' : 'btn-outline-primary'}`}
                          onClick={() => handleShowVacaciones(emp.id)}
                        >
                          {vacacionesVigentes[emp.id] ? (
                            <>
                              <strong>En Vacaciones</strong>
                              <br />
                              <small>{new Date(vacacionesVigentes[emp.id].desde).toLocaleDateString('es-CL')} - {new Date(vacacionesVigentes[emp.id].hasta || vacacionesVigentes[emp.id].retorno).toLocaleDateString('es-CL')}</small>
                            </>
                          ) : 'Ver Vacaciones'}
                        </button>
                      </td>
                      <td>
                        <button
                          className={`btn btn-sm ${licenciasVigentes[emp.id] ? 'btn-success' : 'btn-outline-success'}`}
                          onClick={() => handleShowLicencias(emp.id)}
                        >
                          {licenciasVigentes[emp.id] ? (
                            <>
                              <strong>Con Licencia</strong>
                              <br />
                              <small>{new Date(licenciasVigentes[emp.id].desde).toLocaleDateString('es-CL')} - {new Date(licenciasVigentes[emp.id].hasta).toLocaleDateString('es-CL')}</small>
                            </>
                          ) : 'Ver Licencias'}
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
          {/* Paginación dentro del container principal */}
          {totalPages > 1 && (
            <nav>
              <ul className="pagination justify-content-center mt-4">
                {/* Botón para ir al bloque anterior */}
                {startPage > 1 && (
                  <li className="page-item">
                    <button className="page-link" onClick={() => handlePageChange(startPage - 1)}>&laquo;</button>
                  </li>
                )}
                {pageNumbers.map(page => (
                  <li key={page} className={`page-item${currentPage === page ? ' active' : ''}`}>
                    <button className="page-link" onClick={() => handlePageChange(page)}>
                      {page}
                    </button>
                  </li>
                ))}
                {/* Botón para ir al bloque siguiente */}
                {endPage < totalPages && (
                  <li className="page-item">
                    <button className="page-link" onClick={() => handlePageChange(endPage + 1)}>&raquo;</button>
                  </li>
                )}
              </ul>
            </nav>
          )}
        </>
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