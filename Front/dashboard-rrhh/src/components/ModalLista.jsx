import React from 'react';

export default function ModalLista({ show, onClose, title, items, tipo }) {
  if (!show) return null;
  return (
    <div className="modal show d-block" tabIndex="-1" style={{ background: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">{title}</h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          <div className="modal-body">
            {items && items.length > 0 ? (
              <ul>
                {items.map(item => (
                  <li key={item.id}>
                    Desde: {item.startDate} - Hasta: {item.endDate}
                  </li>
                ))}
              </ul>
            ) : (
              <p>No hay {tipo} registradas.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}