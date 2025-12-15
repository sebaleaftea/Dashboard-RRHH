import React, { useEffect, useState } from 'react';
import axios from 'axios';
import GraficoRangoEtareo from '../components/GraficoRangoEtareo';
import GraficoGenero from '../components/GraficoGenero';
import GraficoDiscapacidad from '../components/GraficoDiscapacidad';

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

export default function Home() {
  const [empleados, setEmpleados] = useState([]);

  useEffect(() => {
    axios.get(API_URL).then(res => setEmpleados(res.data));
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
            <h5>Item que podemos agregar...por verse</h5>
            {/*  */}
            <p className="h2"></p>
          </div>
        </div>
      </div>
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
    </div>
  );
}