import { useEffect, useState } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8082/api/db/empleados/activos';

export default function ReporteDiscapacidad() {
  const [data, setData] = useState([]);
  const [busqueda, setBusqueda] = useState('');

  // Helper function para determinar si tiene discapacidad
  // Funciona tanto con boolean (actual) como con string (después de migración)
  const tieneDiscapacidad = (discapacidad) => {
    if (discapacidad === null || discapacidad === undefined) return false;
    if (typeof discapacidad === 'boolean') return discapacidad;
    if (typeof discapacidad === 'string') return discapacidad.trim().length > 0;
    return false;
  };

  useEffect(() => {
    axios.get(API_URL).then(res => {
      const empleados = (Array.isArray(res.data) ? res.data : []).map(it => ({
        sucursal: it.sucursalNombre || 'Sin sucursal',
        sexo: it.sexo || null,
        fecha_nacimiento: it.fecha_nacimiento || null,
        discapacidad: it.discapacidad || null,
      }));
      // Agrupa por sucursal
      const sucursales = {};
      empleados.forEach(e => {
        const key = e.sucursal || 'Sin sucursal';
        if (!sucursales[key]) {
          sucursales[key] = { si: 0, no: 0, total: 0 };
        }
        if (tieneDiscapacidad(e.discapacidad)) {
          sucursales[key].si++;
        } else {
          sucursales[key].no++;
        }
        sucursales[key].total++;
      });

      // Calcula porcentajes
      const arr = Object.entries(sucursales).map(([sucursal, val]) => {
        const pctSi = val.total ? ((val.si / val.total) * 100).toFixed(2) : 0;
        const pctNo = val.total ? ((val.no / val.total) * 100).toFixed(2) : 0;
        return {
          sucursal,
          si: val.si,
          no: val.no,
          pctSi,
          pctNo,
          total: val.total,
        };
      });
      setData(arr);
    });
  }, []);

  // Filtro de búsqueda por sucursal
  const dataFiltrada = data.filter(row =>
    (row.sucursal || '').toLowerCase().includes(busqueda.toLowerCase())
  );

  // Total general (solo de los filtrados)
  const totalSi = dataFiltrada.reduce((sum, d) => sum + d.si, 0);
  const totalNo = dataFiltrada.reduce((sum, d) => sum + d.no, 0);
  const totalPersonas = dataFiltrada.reduce((sum, d) => sum + d.total, 0);
  const pctTotalSi = totalPersonas ? ((totalSi / totalPersonas) * 100).toFixed(2) : 0;
  const pctTotalNo = totalPersonas ? ((totalNo / totalPersonas) * 100).toFixed(2) : 0;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Reporte Discapacidad</h2>
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
              <th>Con Discapacidad</th>
              <th>Sin Discapacidad</th>
              <th>% Con Discapacidad</th>
              <th>% Sin Discapacidad</th>
              <th>Total Personas</th>
            </tr>
          </thead>
          <tbody>
            {dataFiltrada.map(row => (
              <tr key={row.sucursal}>
                <td>{row.sucursal}</td>
                <td>{row.si}</td>
                <td>{row.no}</td>
                <td>{row.pctSi}%</td>
                <td>{row.pctNo}%</td>
                <td>{row.total}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="table-secondary fw-bold">
              <td>Total</td>
              <td>{totalSi}</td>
              <td>{totalNo}</td>
              <td>{pctTotalSi}%</td>
              <td>{pctTotalNo}%</td>
              <td>{totalPersonas}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}