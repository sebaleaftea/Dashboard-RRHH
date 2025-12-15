import { useEffect, useState } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8082/api/v1/employees/all';

// filtro para pasar los cargos antiguos a nuevos
const MAPA_CARGOS = {
  'Vendedor-Reponedor-Cajero': 'Operador de tienda',
  'Vendedor Reponedor Cajero 20': 'Operador de tienda 16',
  'Vendedor Reponedor Cajero 30': 'Operador de tienda 24',
  'Administrador Jr Trainee': 'Administrador de tienda Jr',
  'SUB-ENCARGADO DE TIENDA': 'Administrador de tienda Jr',
  'Operador de tienda Trainee': 'Operador de tienda',
};

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

  useEffect(() => {
    axios.get(API_URL).then(res => {
      const empleados = res.data;

      // llamo a la función de agrupamiento y cálculo
      const agrupado = {};
      empleados.forEach(emp => {
        // mapeo de cargo para traducir a nuevos nombres
        const cargoTraducido = MAPA_CARGOS[emp.cargo] || emp.cargo;
        emp.cargo = cargoTraducido;

        const sucursalKey = emp.sucursal ? emp.sucursal.trim().toLowerCase() : 'sin_sucursal';
        if (!agrupado[sucursalKey]) {
          agrupado[sucursalKey] = {
            nombre: emp.sucursal,
            jefe: emp.jefe,
            empleados: [],
          };
        }
        agrupado[sucursalKey].empleados.push(emp);
      });

      // Calcula dotación y cargos por sucursal
      const sucursalesArr = Object.values(agrupado).map(data => {
        const dotacionVigente = data.empleados.length;
        const dotacionEsperada = 7;
        const desviacion = dotacionEsperada === 0
          ? 0
          : ((dotacionVigente - dotacionEsperada) / dotacionEsperada) * 100;
        // Cuenta cargos actuales
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

  // Filtra sucursales según búsqueda
  const sucursalesFiltradas = sucursales.filter(suc =>
    (suc.nombre || '').toLowerCase().includes(busqueda.toLowerCase())
  );

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Reclutamiento por Sucursal</h2>
      <div className="mb-3">
        <input
          type="text"
          className="form-control"
          placeholder="Buscar sucursal..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
        />
      </div>
      <div className="table-responsive">
        <table className="table table-bordered align-middle">
          <thead className="table-dark">
            <tr>
              <th>Sucursal</th>
              <th>Jefe</th>
              <th>Dotación Vigente</th>
              <th>Dotación Esperada</th>
              <th>Desviación (%)</th>
              <th>Estado</th>
              <th>Detalle de cargos</th>
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
                  ) : (
                    <span className="badge bg-success">Sobre dotación</span>
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
    </div>
  );
}