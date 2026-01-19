import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';

export default function GraficoGenero({ data }) {
  const navigate = useNavigate();
  const [showModal, setShowModal] = useState(false);

  // Verificar que data tenga valores
  if (!data || data.length === 0) {
    return (
      <div>
        <h5>Dotación por Género y Sucursal</h5>
        <p className="text-muted">No hay datos disponibles</p>
      </div>
    );
  }

  const handleChartClick = (e) => {
    // Detener la propagación para que no se active el modal si se hace clic en el gráfico mismo
    e.stopPropagation();
    setShowModal(true);
  };

  const handleGoToReport = () => {
    navigate('/reporte-genero');
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  return (
    <>
      <div style={{ cursor: 'pointer' }} onClick={handleChartClick}>
        <h5>Dotación por Género y Sucursal</h5>
        <ResponsiveContainer width="100%" height={350}>
          <BarChart data={data}>
            <XAxis dataKey="sucursal" angle={-45} textAnchor="end" height={80} />
            <YAxis />
            <Tooltip />
            <Legend 
              wrapperStyle={{ 
                fontWeight: 'bold', 
                fontSize: '14px',
                paddingTop: '10px'
              }}
              iconType="rect"
              iconSize={14}
            />
            <Bar dataKey="hombres" fill="#4A90E2" name="Hombres" stackId="a" />
            <Bar dataKey="mujeres" fill="#E94E77" name="Mujeres" stackId="a" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Modal para vista ampliada */}
      {showModal && (
        <div 
          className="modal show d-block" 
          tabIndex="-1" 
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
          onClick={handleCloseModal}
        >
          <div 
            className="modal-dialog modal-xl modal-dialog-centered"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Dotación por Género y Sucursal - Vista Ampliada</h5>
                <button 
                  type="button" 
                  className="btn-close" 
                  onClick={handleCloseModal}
                  aria-label="Close"
                ></button>
              </div>
              <div className="modal-body">
                <ResponsiveContainer width="100%" height={500}>
                  <BarChart data={data}>
                    <XAxis 
                      dataKey="sucursal" 
                      angle={-45} 
                      textAnchor="end" 
                      height={100}
                      style={{ fontSize: '12px' }}
                    />
                    <YAxis />
                    <Tooltip 
                      contentStyle={{ backgroundColor: 'white', border: '1px solid #ccc' }}
                    />
                    <Legend 
                      wrapperStyle={{ 
                        fontWeight: 'bold', 
                        fontSize: '16px',
                        paddingTop: '20px'
                      }}
                      iconType="rect"
                      iconSize={16}
                    />
                    <Bar dataKey="hombres" fill="#4A90E2" name="Hombres" stackId="a" />
                    <Bar dataKey="mujeres" fill="#E94E77" name="Mujeres" stackId="a" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div className="modal-footer">
                <button 
                  type="button" 
                  className="btn btn-primary"
                  onClick={handleGoToReport}
                >
                  Ir a la Página
                </button>
                <button 
                  type="button" 
                  className="btn btn-secondary" 
                  onClick={handleCloseModal}
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}