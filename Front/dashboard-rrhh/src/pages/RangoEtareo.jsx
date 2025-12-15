import { useEffect, useState } from 'react';
import axios from 'axios';

const API_URL = 'http://localhost:8082/api/v1/employees/all';

function calcularEdad(fechaNacimiento) {
  if (!fechaNacimiento) return 0;
  const hoy = new Date();
  const nacimiento = new Date(fechaNacimiento);
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  const m = hoy.getMonth() - nacimiento.getMonth();
  if (m < 0 || (m === 0 && hoy.getDate() < nacimiento.getDate())) {
    edad--;
  }
  return edad;
}

export default function RangoEtareo() {
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
          sucursales[key] = { edades: [], cantidad: 0 };
        }
        const edad = calcularEdad(e.fecha_nacimiento);
        sucursales[key].edades.push(edad);
        sucursales[key].cantidad++;
      });

      // Calcula promedio de edad
      const arr = Object.entries(sucursales).map(([sucursal, val]) => {
        const promedioEdad =
          val.cantidad > 0
            ? (val.edades.reduce((sum, edad) => sum + edad, 0) / val.cantidad).toFixed(2)
            : 0;
        return {
          sucursal,
          cantidad: val.cantidad,
          promedioEdad,
        };
      });
      setData(arr);
    });
  }, []);

  // Filtro de búsqueda por sucursal
  const dataFiltrada = data.filter(row =>
    (row.sucursal || '').toLowerCase().includes(busqueda.toLowerCase())
  );

  // Totales generales (solo de los filtrados)
  const totalPersonas = dataFiltrada.reduce((sum, d) => sum + d.cantidad, 0);
  const promedioGeneral =
    totalPersonas > 0
      ? (
          dataFiltrada.reduce((sum, d) => sum + d.promedioEdad * d.cantidad, 0) /
          totalPersonas
        ).toFixed(2)
      : 0;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Reporte Rango Etario</h2>
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
              <th>Cantidad de Personas</th>
              <th>Promedio de Edad</th>
            </tr>
          </thead>
          <tbody>
            {dataFiltrada.map(row => (
              <tr key={row.sucursal}>
                <td>{row.sucursal}</td>
                <td>{row.cantidad}</td>
                <td>{row.promedioEdad}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="table-secondary fw-bold">
              <td>Total</td>
              <td>{totalPersonas}</td>
              <td>{promedioGeneral}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}