import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from '../components/Layout';
import Home from '../pages/Home';
import Empleados from '../pages/Empleados';
import Login from '../pages/Login';
import Reclutamiento from '../pages/Reclutamiento';
import ReporteDiscapacidad from '../pages/ReporteDiscapacidad';
import ReporteGenero from '../pages/ReporteGenero';
import RangoEtareo from '../pages/RangoEtareo';

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route element={<Layout />}>
          <Route path="/home" element={<Home />} />
          <Route path="/empleados" element={<Empleados />} />
          <Route path="/reclutamiento" element={<Reclutamiento />} />
          <Route path="/reporte-discapacidad" element={<ReporteDiscapacidad />} />
          <Route path="/reporte-genero" element={<ReporteGenero />} />
          <Route path="/reporte-edades" element={<RangoEtareo />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}