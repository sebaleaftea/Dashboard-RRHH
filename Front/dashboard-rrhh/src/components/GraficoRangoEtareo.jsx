import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';

export default function GraficoRangoEtareo({ data }) {
  const navigate = useNavigate();

  return (
    <div style={{ cursor: 'pointer' }} onClick={() => navigate('/reporte-edades')}>
      <h5>Rango Etario</h5>
      <ResponsiveContainer width="100%" height={350}>
        <BarChart data={data}>
          <XAxis dataKey="label" />
          <YAxis />
          <Tooltip />
          <Bar dataKey="cantidad" fill="#45429cff" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}