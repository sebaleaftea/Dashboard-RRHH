import { useEffect, useState } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8082/api/v1/employees/all';

export default function ReporteGenero() {
  const [data, setData] = useState([]);
  const [busqueda, setBusqueda] = useState('');

  useEffect(() => {
    axios.get(API_URL).then(res => {
      const empleados = res.data;
      // Agrupa por sucursal
      const sucursales = {};
      empleados.forEach(e => {
        const key = e.sucursal || 'Sin sucursal';
        if (!sucursales[key]) {
          sucursales[key] = { F: 0, M: 0, total: 0 };
        }
        if (e.sexo === 'F') {
          sucursales[key].F++;
        } else if (e.sexo === 'M') {
          sucursales[key].M++;
        }
        sucursales[key].total++;
      });

      // Calcula porcentajes
      const arr = Object.entries(sucursales).map(([sucursal, val]) => {
        const pctF = val.total ? ((val.F / val.total) * 100).toFixed(2) : 0;
        const pctM = val.total ? ((val.M / val.total) * 100).toFixed(2) : 0;
        return {
          sucursal,
          F: val.F,
          M: val.M,
          pctF,
          pctM,
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
  const totalF = dataFiltrada.reduce((sum, d) => sum + d.F, 0);
  const totalM = dataFiltrada.reduce((sum, d) => sum + d.M, 0);
  const totalPersonas = dataFiltrada.reduce((sum, d) => sum + d.total, 0);
  const pctTotalF = totalPersonas ? ((totalF / totalPersonas) * 100).toFixed(2) : 0;
  const pctTotalM = totalPersonas ? ((totalM / totalPersonas) * 100).toFixed(2) : 0;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Reporte Género</h2>
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
              <th>F</th>
              <th>M</th>
              <th>% F</th>
              <th>% M</th>
              <th>Total Personas</th>
            </tr>
          </thead>
          <tbody>
            {dataFiltrada.map(row => (
              <tr key={row.sucursal}>
                <td>{row.sucursal}</td>
                <td>{row.F}</td>
                <td>{row.M}</td>
                <td>{row.pctF}%</td>
                <td>{row.pctM}%</td>
                <td>{row.total}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="table-secondary fw-bold">
              <td>Total</td>
              <td>{totalF}</td>
              <td>{totalM}</td>
              <td>{pctTotalF}%</td>
              <td>{pctTotalM}%</td>
              <td>{totalPersonas}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}