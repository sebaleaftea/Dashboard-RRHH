
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import GraficoRangoEtareo from '../components/GraficoRangoEtareo';
import GraficoGenero from '../components/GraficoGenero';
import GraficoDiscapacidad from '../components/GraficoDiscapacidad';
import GraficoSerieDiaria from '../components/GraficoSerieDiaria';
import ModalLista from '../components/ModalLista';
import { normalizarCargo } from '../utils/cargos';
import { API_URLS } from '../config/api';

// Usaremos contratos activos como fuente única de verdad
const API_URL = `${API_URLS.EMPLOYEE_SERVICE}/api/db/empleados/activos`;
const DB_BASE = `${API_URLS.EMPLOYEE_SERVICE}/api/db`;

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

export default function Home() {
  const [empleados, setEmpleados] = useState([]);
  const [stats, setStats] = useState(null);
  const [serieVacaciones, setSerieVacaciones] = useState([]);
  const [serieLicencias, setSerieLicencias] = useState([]);
  // Ausentismos
  const [vacacionesVigentes, setVacacionesVigentes] = useState([]);
  const [licenciasVigentes, setLicenciasVigentes] = useState([]);
  const [modalAusentismo, setModalAusentismo] = useState({ show: false, tipo: '', data: [] });

  useEffect(() => {
    // Contratos activos para dotación y cargos
    axios.get(API_URL).then(res => {
      const items = Array.isArray(res.data) ? res.data : [];
      const empleadosActivos = items.map(it => ({
        id: it.empleadoId,
        rut: it.rut,
        nombre: it.nombre,
        sucursal: it.sucursalNombre || 'Sin sucursal',
        cargo: normalizarCargo(it.cargo),
        sexo: it.sexo || null,
        fecha_nacimiento: it.fecha_nacimiento || null,
        discapacidad: it.discapacidad || null,
      }));
      setEmpleados(empleadosActivos);
    });

    // Stats generales (incluye métricas de hoy)
    axios.get(`${DB_BASE}/stats`).then(res => setStats(res.data));
    // Series diarias para gráficos y ausentismos
    axios.get(`${DB_BASE}/metrics/vacaciones/daily?days=1`).then(res => {
      setSerieVacaciones(res.data);
      // Si el backend entrega la lista de personas en el último día:
      if (Array.isArray(res.data) && res.data.length > 0 && res.data[0].personas) {
        setVacacionesVigentes(res.data[0].personas);
      } else {
        // Si solo entrega el total, no se puede mostrar el detalle
        setVacacionesVigentes([]);
      }
    });
    axios.get(`${DB_BASE}/metrics/licencias/daily?days=1`).then(res => {
      setSerieLicencias(res.data);
      if (Array.isArray(res.data) && res.data.length > 0 && res.data[0].personas) {
        setLicenciasVigentes(res.data[0].personas);
      } else {
        setLicenciasVigentes([]);
      }
    });
  }, []);

  // Rango etario
  const rangos = [
    { label: '18-25', min: 18, max: 25 },
    { label: '26-35', min: 26, max: 35 },
    { label: '36-45', min: 36, max: 45 },
    { label: '46-60', min: 46, max: 60 },
    { label: '60+', min: 61, max: 200 },
  ];
  const dataRangoEtareo = rangos.map(rango => ({
    label: rango.label,
    cantidad: empleados.filter(e => {
      const edad = calcularEdad(e.fecha_nacimiento);
      return edad >= rango.min && edad <= rango.max;
    }).length,
  }));

  // Dotación género por tienda
  const sucursales = [...new Set(empleados.map(e => e.sucursal))];
  // Si no tenemos sexo, mostramos 0/0 para evitar errores en el gráfico
  const dataGenero = sucursales.map(suc => {
    const empleadosSucursal = empleados.filter(e => e.sucursal === suc);
    const hombres = empleadosSucursal.filter(e => e.sexo === 'M').length;
    const mujeres = empleadosSucursal.filter(e => e.sexo === 'F').length;
    return { sucursal: suc, hombres, mujeres };
  });

  // Desviación total general
  const dotacionEsperadaPorSucursal = 7; 
  const dotacionEsperadaTotal = sucursales.length * dotacionEsperadaPorSucursal;
  const dotacionVigenteTotal = empleados.length;
  const desviacionTotal = dotacionEsperadaTotal === 0
    ? 0
    : ((dotacionVigenteTotal - dotacionEsperadaTotal) / dotacionEsperadaTotal) * 100;

  //Discapacidad
  const conDiscapacidad = empleados.filter(e => e.discapacidad).length;
  const sinDiscapacidad = empleados.length - conDiscapacidad;
  const dataDiscapacidad = [
    { label: 'Con discapacidad', cantidad: conDiscapacidad },
    { label: 'Sin discapacidad', cantidad: sinDiscapacidad },
  ];

  // Resumen de cargos clave (vendedores y subgerentes), usando cargos mapeados
  const totalVendedor16 = empleados.filter(e => e.cargo === 'Operador de tienda 16').length;
  const totalVendedor24 = empleados.filter(e => e.cargo === 'Operador de tienda 24').length;
  const totalVendedor = empleados.filter(e => e.cargo === 'Operador de tienda').length;
  const totalSubgerente = empleados.filter(e => e.cargo === 'Administrador de tienda Jr').length;
  const totalAdminTienda = empleados.filter(e => e.cargo === 'Administrador de tienda').length;

  return (
    <div className="container mt-4">
      <h2 className="mb-4">Dashboard Principal</h2>
      <div className="row g-4 mb-4">
        <div className="col-md-4">
          <div className="p-3 border rounded bg-light">
            <h5>Total Empleados</h5>
            <p className="h2">{empleados.length}</p>
          </div>
        </div>
        <div className="col-md-4">
          <div className="p-3 border rounded bg-light">
            <h5>% Desviación Total</h5>
            <p className="h2">{desviacionTotal.toFixed(2)}%</p>
          </div>
        </div>
        <div className="col-md-4">
          <div className="p-3 border rounded bg-light">
            <h5>Cargos clave (mapeados)</h5>
            <div className="d-flex flex-column gap-1">
              <span>Vendedor PT 16: <strong>{totalVendedor16}</strong></span>
              <span>Vendedor PT 24: <strong>{totalVendedor24}</strong></span>
              <span>Vendedor Full: <strong>{totalVendedor}</strong></span>
              <span>Administrador de tienda Jr: <strong>{totalSubgerente}</strong></span>
              <span>Administrador de tienda: <strong>{totalAdminTienda}</strong></span>
            </div>
          </div>
        </div>
      </div>
      {stats && (
        <div className="row g-4 mb-4">
          <div className="col-md-4">
          </div>
          <div className="col-md-4">
          </div>
        </div>
      )}
      <div className="row g-4">
        <div className="col-md-4">
          <div className="p-3 border rounded bg-white">
            <GraficoRangoEtareo data={dataRangoEtareo} />
          </div>
        </div>
        <div className="col-md-4">
          <div className="p-3 border rounded bg-white">
            <GraficoGenero data={dataGenero} />
          </div>
        </div>
        <div className="col-md-4">
          <div className="p-3 border rounded bg-white">
            <GraficoDiscapacidad data={dataDiscapacidad} />
          </div>
        </div>
      </div>
      <div className="row g-4 mt-4">
        <div className="col-md-6">
          <div className="p-3 border rounded bg-white">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="fw-bold">Vacaciones vigentes: {Array.isArray(vacacionesVigentes) ? vacacionesVigentes.length : 0}</span>
              <button className="btn btn-sm btn-outline-info" onClick={() => setModalAusentismo({ show: true, tipo: 'vacaciones', data: vacacionesVigentes })} disabled={!vacacionesVigentes.length}>
                Ver detalle
              </button>
            </div>
            <GraficoSerieDiaria title="Vacaciones (últimos 14 días)" data={serieVacaciones} color="#5ab67dff" />
          </div>
        </div>
        <div className="col-md-6">
          <div className="p-3 border rounded bg-white">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="fw-bold">Licencias vigentes: {Array.isArray(licenciasVigentes) ? licenciasVigentes.length : 0}</span>
              <button className="btn btn-sm btn-outline-warning" onClick={() => setModalAusentismo({ show: true, tipo: 'licencias', data: licenciasVigentes })} disabled={!licenciasVigentes.length}>
                Ver detalle
              </button>
            </div>
            <GraficoSerieDiaria title="Licencias (últimos 14 días)" data={serieLicencias} color="#e38b00" />
          </div>
        </div>
      </div>

      {/* Modal detalle ausentismos */}
      {modalAusentismo.show && (
        <div className="modal show d-block" tabIndex="-1" style={{ background: 'rgba(0,0,0,0.3)' }}>
          <div className="modal-dialog modal-lg">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">
                  {modalAusentismo.tipo === 'vacaciones' ? 'Personas con vacaciones vigentes' : 'Personas con licencias vigentes'}
                </h5>
                <button type="button" className="btn-close" onClick={() => setModalAusentismo({ show: false, tipo: '', data: [] })}></button>
              </div>
              <div className="modal-body">
                {modalAusentismo.data.length === 0 ? (
                  <div className="alert alert-info">No hay personas con ausentismo vigente.</div>
                ) : (
                  <table className="table table-sm">
                    <thead>
                      <tr>
                        <th>Nombre</th>
                        <th>Sucursal</th>
                        <th>Desde</th>
                        <th>Hasta</th>
                      </tr>
                    </thead>
                    <tbody>
                      {modalAusentismo.data.map((a, i) => (
                        <tr key={i}>
                          <td>{a.nombre}</td>
                          <td>{a.sucursal}</td>
                          <td>{a.desde ? new Date(a.desde).toLocaleDateString('es-CL') : ''}</td>
                          <td>{a.hasta ? new Date(a.hasta).toLocaleDateString('es-CL') : ''}</td>
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