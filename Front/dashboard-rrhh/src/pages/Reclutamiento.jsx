
import { useEffect, useState } from 'react';
import axios from 'axios';
import { normalizarCargo } from '../utils/cargos';
import { API_URLS } from '../config/api';

// Utilidad para obtener ausentismos por sucursal (vacaciones y licencias vigentes)
async function fetchAusentismosPorSucursal(sucursalNombre) {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  // Traer vacaciones y licencias vigentes de hoy
  const [vacRes, licRes] = await Promise.all([
    axios.get(`${API_URLS.EMPLOYEE_SERVICE}/api/db/metrics/vacaciones/daily?days=1`),
    axios.get(`${API_URLS.EMPLOYEE_SERVICE}/api/db/metrics/licencias/daily?days=1`)
  ]);
  const vacaciones = Array.isArray(vacRes.data) && vacRes.data.length > 0 && vacRes.data[0].personas ? vacRes.data[0].personas : [];
  const licencias = Array.isArray(licRes.data) && licRes.data.length > 0 && licRes.data[0].personas ? licRes.data[0].personas : [];
  // Filtrar por sucursal
  // Solo los ausentismos vigentes (hoy dentro del rango)
  const isVigente = (desde, hasta, retorno) => {
    const d = desde ? new Date(desde) : null;
    const h = hasta ? new Date(hasta) : null;
    const r = retorno ? new Date(retorno) : null;
    if (!d) return false;
    // Si no hay hasta, usar retorno
    const fin = h || r;
    if (!fin) return hoy >= d;
    return hoy >= d && hoy <= fin;
  };
  const ausentes = [
    ...vacaciones.filter(v => (v.sucursal || 'Sin sucursal') === (sucursalNombre || 'Sin sucursal') && isVigente(v.desde, v.hasta, v.retorno)).map(v => ({
      nombre: v.nombre,
      sucursal: v.sucursal,
      desde: v.desde,
      hasta: v.hasta,
      tipo: 'Vacaciones'
    })),
    ...licencias.filter(l => (l.sucursal || 'Sin sucursal') === (sucursalNombre || 'Sin sucursal') && isVigente(l.desde, l.hasta)).map(l => ({
      nombre: l.nombre,
      sucursal: l.sucursal,
      desde: l.desde,
      hasta: l.hasta,
      tipo: 'Licencia'
    }))
  ];
  return ausentes;
}

// Usamos el endpoint normalizado de contratos activos desde la BDD
const API_URL = `${API_URLS.EMPLOYEE_SERVICE}/api/db/contratos/activos`;

// El mapeo de cargos ahora se comparte en utils/cargos.js

//Cargos minimos requeridos e ideales por sucursales
const CARGOS_REQUERIDOS = [
  { cargo: 'Operador de tienda 16', requeridos: 2 },
  { cargo: 'Operador de tienda 24', requeridos: 1 },
  { cargo: 'Operador de tienda', requeridos: 2 }, 
  { cargo: 'Administrador de tienda Jr', requeridos: 1 },
  { cargo: 'Administrador de tienda', requeridos: 1 },
];


export default function Reclutamiento() {
  const [sucursales, setSucursales] = useState([]);
  const [detalle, setDetalle] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [estadoFiltro, setEstadoFiltro] = useState('');
  // Estado para ausentismos
  const [modalAusentismos, setModalAusentismos] = useState({ show: false, data: [], sucursal: '' });

  useEffect(() => {
    axios.get(API_URL).then(res => {
      const contratos = Array.isArray(res.data) ? res.data : [];

      // Adaptamos a una lista de "empleados activos" con los campos necesarios (por centro de costo)
      const empleados = contratos.map(c => ({
        centro: c.centroCostoNombre || 'Sin centro de costo',
        jefe: c.jefeNombre || '',
        cargo: normalizarCargo(c.cargo),
      }));

      // Agrupamiento por centro de costo
      const agrupado = {};
      empleados.forEach(emp => {
        const centroKey = emp.centro ? emp.centro.trim().toLowerCase() : 'sin_centro';
        if (!agrupado[centroKey]) {
          agrupado[centroKey] = {
            nombre: emp.centro,
            jefe: '',
            empleados: [],
          };
        }
        // preferimos un jefe no vacío; si hay varios, nos quedamos con el primero válido
        if (!agrupado[centroKey].jefe && emp.jefe) {
          agrupado[centroKey].jefe = emp.jefe;
        }
        agrupado[centroKey].empleados.push(emp);
      });

      // Calcula dotación y cargos por centro de costo
      const requeridosTotales = CARGOS_REQUERIDOS.reduce((acc, it) => acc + (it.requeridos || 0), 0);
      const sucursalesArr = Object.values(agrupado).map(data => {
        const dotacionVigente = data.empleados.length;
        const dotacionEsperada = requeridosTotales; // suma de cargos requeridos
        const desviacion = dotacionEsperada === 0
          ? 0
          : ((dotacionVigente - dotacionEsperada) / dotacionEsperada) * 100;
        // Cuenta cargos actuales (post-mapeo)
        const cargosActuales = {};
        data.empleados.forEach(emp => {
          cargosActuales[emp.cargo] = (cargosActuales[emp.cargo] || 0) + 1;
        });
        return {
          nombre: data.nombre,
          jefe: data.jefe,
          dotacionVigente,
          dotacionEsperada,
          desviacion,
          cargosActuales,
        };
      });
      setSucursales(sucursalesArr);
    });
  }, []);

  // Filtra centros según búsqueda y estado (dropdown)
  const sucursalesFiltradas = sucursales.filter(suc => {
    const nombre = (suc.nombre || '').toLowerCase();
    const estado = suc.desviacion < 0
      ? 'requiere reclutar'
      : suc.desviacion === 0
        ? 'dotación cumplida'
        : 'sobredotación';
    const matchNombre = nombre.includes(busqueda.toLowerCase());
    const matchEstado = estadoFiltro ? estado === estadoFiltro : true;
    return matchNombre && matchEstado;
  });


  return (
    <div className="container mt-4">
      <h2 className="mb-4">Reclutamiento por Centro de Costo</h2>
      <div className="row mb-3">
        <div className="col-md-8 mb-2 mb-md-0">
          <input
            type="text"
            className="form-control"
            placeholder="Buscar centro de costo..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
          />
        </div>
        <div className="col-md-4">
          <select
            className="form-select"
            value={estadoFiltro}
            onChange={e => setEstadoFiltro(e.target.value)}
          >
            <option value="">Todos los estados</option>
            <option value="requiere reclutar">Requiere reclutar</option>
            <option value="dotación cumplida">Dotación cumplida</option>
            <option value="sobredotación">Sobredotación</option>
          </select>
        </div>
      </div>
      <div className="table-responsive">
        <table className="table table-bordered align-middle">
          <thead className="table-dark">
            <tr>
              <th>Centro de Costo</th>
              <th>Jefe</th>
              <th>Dotación Vigente</th>
              <th>Dotación Esperada</th>
              <th>Desviación (%)</th>
              <th>Estado</th>
              <th>Detalle de cargos</th>
              <th>Ver ausentismos</th>
            </tr>
          </thead>
          <tbody>
            {sucursalesFiltradas.map(suc => (
              <tr key={suc.nombre}>
                <td>{suc.nombre}</td>
                <td>{suc.jefe}</td>
                <td>{suc.dotacionVigente}</td>
                <td>{suc.dotacionEsperada}</td>
                <td>{suc.desviacion.toFixed(2)}%</td>
                <td>
                  {suc.desviacion < 0 ? (
                    <span className="badge bg-danger">Requiere reclutar</span>
                  ) : suc.desviacion === 0 ? (
                    <span className="badge bg-success">Dotación Cumplida</span>
                  ) : (
                    <span className="badge bg-warning text-dark">Sobre dotación</span>
                  )}
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-info"
                    onClick={() => setDetalle(suc)}
                  >
                    Ver cargos
                  </button>
                </td>
                <td>
                  <button
                    className="btn btn-sm btn-warning"
                    onClick={async () => {
                      // Obtener ausentismos por sucursal (vacaciones y licencias vigentes)
                      const ausentes = await fetchAusentismosPorSucursal(suc.nombre);
                      setModalAusentismos({ show: true, data: ausentes, sucursal: suc.nombre });
                    }}
                  >
                    Ver ausentismos
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal de detalle de cargos */}
      {detalle && (
        <div className="modal show d-block" tabIndex="-1" style={{ background: 'rgba(0,0,0,0.3)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Detalle de cargos - {detalle.nombre}</h5>
                <button type="button" className="btn-close" onClick={() => setDetalle(null)}></button>
              </div>
              <div className="modal-body">
                <table className="table table-sm">
                  <thead>
                    <tr>
                      <th>Cargo</th>
                      <th>Requeridos</th>
                      <th>Vigentes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {/* Cargos requeridos */}
                    {CARGOS_REQUERIDOS.map(cargo => (
                      <tr key={cargo.cargo}>
                        <td>{cargo.cargo}</td>
                        <td>{cargo.requeridos}</td>
                        <td>{detalle.cargosActuales[cargo.cargo] || 0}</td>
                      </tr>
                    ))}
                    {/* Cargos extra no requeridos pero presentes */}
                    {Object.entries(detalle.cargosActuales)
                      .filter(([cargo]) => !CARGOS_REQUERIDOS.some(req => req.cargo === cargo))
                      .map(([cargo, cantidad]) => (
                        <tr key={cargo}>
                          <td>{cargo}</td>
                          <td>-</td>
                          <td>{cantidad}</td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal de ausentismos */}
      {modalAusentismos.show && (
        <div className="modal show d-block" tabIndex="-1" style={{ background: 'rgba(0,0,0,0.3)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Ausentismos - {modalAusentismos.sucursal}</h5>
                <button type="button" className="btn-close" onClick={() => setModalAusentismos({ show: false, data: [], sucursal: '' })}></button>
              </div>
              <div className="modal-body">
                {modalAusentismos.data.length === 0 ? (
                  <div className="alert alert-info">No hay personas con ausentismo vigente en esta tienda.</div>
                ) : (
                  <table className="table table-sm">
                    <thead>
                      <tr>
                        <th>Nombre</th>
                        <th>Sucursal</th>
                        <th>Desde</th>
                        <th>Hasta</th>
                        <th>Tipo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {modalAusentismos.data.map((a, i) => (
                        <tr key={i}>
                          <td>{a.nombre}</td>
                          <td>{a.sucursal}</td>
                          <td>{a.desde ? new Date(a.desde).toLocaleDateString('es-CL') : ''}</td>
                          <td>{a.hasta ? new Date(a.hasta).toLocaleDateString('es-CL') : ''}</td>
                          <td>{a.tipo}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}