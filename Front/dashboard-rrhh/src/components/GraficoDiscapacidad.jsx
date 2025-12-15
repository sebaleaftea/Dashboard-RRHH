import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';

const COLORS = ['#0088FE', '#FF8042'];

export default function GraficoDiscapacidad({ data }) {
  const navigate = useNavigate();

  return (
    <div style={{ cursor: 'pointer' }} onClick={() => navigate('/reporte-discapacidad')}>
      <h5>Discapacidad</h5>
      <ResponsiveContainer width="100%" height={350}>
        <PieChart>
          <Pie
            data={data}
            dataKey="cantidad"
            nameKey="label"
            cx="50%"
            cy="50%"
            outerRadius={120}
            label
          >
            {data.map((entry, idx) => (
              <Cell key={`cell-${idx}`} fill={COLORS[idx % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}