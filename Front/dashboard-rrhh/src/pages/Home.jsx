import React from 'react';

export default function Home() {
  return (
    <>
      <h2 className="mb-4">Dashboard Principal</h2>
      <div className="row g-4">
        <div className="col-md-4">
          <div className="p-3 border rounded bg-light">
            <h5>Total Empleados</h5>
            <p className="h2">124</p>
          </div>
        </div>
        <div className="col-md-4">
          <div className="p-3 border rounded bg-light">
            <h5>Nuevos Ingresos</h5>
            <p className="h2">3</p>
          </div>
        </div>
        <div className="col-md-4">
          <div className="p-3 border rounded bg-light">
            <h5>Solicitudes Pendientes</h5>
            <p className="h2">12</p>
          </div>
        </div>
      </div>
    </>
  );
}