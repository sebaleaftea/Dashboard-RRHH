import React from 'react';

export default function ModalLista({ show, onClose, title, items, tipo }) {
  if (!show) return null;

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('es-CL');
  };

  return (
    <div className="modal show d-block" tabIndex="-1" style={{ background: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-lg">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">{title}</h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          <div className="modal-body">
            {items && items.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-striped table-sm">
                  <thead>
                    <tr>
                      {tipo === 'vacaciones' ? (
                        <>
                          <th>Desde</th>
                          <th>Hasta</th>
                          <th>Retorno</th>
                          <th>Días</th>
                          <th>Tipo</th>
                        </>
                      ) : (
                        <>
                          <th>Desde</th>
                          <th>Hasta</th>
                          <th>Días</th>
                          <th>Tipo</th>
                        </>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {items.map(item => (
                      <tr key={item.id}>
                        {tipo === 'vacaciones' ? (
                          <>
                            <td>{formatDate(item.desde)}</td>
                            <td>{formatDate(item.hasta)}</td>
                            <td>{formatDate(item.retorno)}</td>
                            <td>{item.dias || 0}</td>
                            <td>{item.tipo || 'N/A'}</td>
                          </>
                        ) : (
                          <>
                            <td>{formatDate(item.desde)}</td>
                            <td>{formatDate(item.hasta)}</td>
                            <td>{item.dias || 0}</td>
                            <td>{item.tipo || 'N/A'}</td>
                          </>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p>No hay {tipo} registradas.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}