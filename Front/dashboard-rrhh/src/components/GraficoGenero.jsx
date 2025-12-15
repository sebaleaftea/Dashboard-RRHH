import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';

export default function GraficoGenero({ data }) {
  const navigate = useNavigate();

  return (
    <div style={{ cursor: 'pointer' }} onClick={() => navigate('/reporte-genero')}>
      <h5>Dotación por Género y Sucursal</h5>
      <ResponsiveContainer width="100%" height={350}>
        <BarChart data={data}>
          <XAxis dataKey="sucursal" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey="hombres" fill="#655fd8ff" name="Hombres" />
          <Bar dataKey="mujeres" fill="#5ab67dff" name="Mujeres" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}