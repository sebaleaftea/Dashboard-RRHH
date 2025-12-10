import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/sidebar.css';

export function Sidebar({ isOpen, onClose }) {
  const navigate = useNavigate();

  const items = [
    { key: 'dashboard', icon: 'bi-speedometer2', label: 'Dashboard', path: '/home' },
    { key: 'empleados', icon: 'bi-people', label: 'Empleados', path: '/empleados' },
    { key: 'reportes', icon: 'bi-bar-chart', label: 'Reportes', path: '/reportes' },
    { key: 'item x', icon: 'bi-bar-chart', label: 'item x', path: '#' },
    { key: 'item y', icon: 'bi-bar-chart', label: 'item y', path: '#' },
  ];

  const handleNavigation = (path) => {
    if (path && path !== '#') {
      navigate(path);
      if (window.innerWidth < 992) onClose();
    }
  };

  // Detecta ruta activa para resaltar el botón
  const currentPath = window.location.pathname;

  return (
    <>
      {isOpen && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-25 d-lg-none"
          style={{ zIndex: 1040 }}
          onClick={onClose}
        />
      )}

      <aside
        className="bg-white border-end vh-100 position-fixed top-0 start-0 sidebar-fixed sidebar-app"
        style={{
          width: '280px',
          zIndex: 1041,
          transform: isOpen ? 'translateX(0)' : 'translateX(-100%)',
          transition: 'transform 300ms ease-in-out',
        }}
        aria-label="Sidebar"
      >
        <div className="d-flex align-items-center justify-content-between px-3 py-3 border-bottom sidebar-header">
          <span className="fw-semibold">Panel RRHH</span>
          <button className="btn btn-light d-lg-none" onClick={onClose} aria-label="Cerrar menú">
            <i className="bi bi-x-lg"></i>
          </button>
        </div>

        <nav className="nav flex-column py-2">
          {items.map((item) => (
            <button
              key={item.key}
              className={`btn text-start rounded-0 px-3 py-2 ${
                currentPath === item.path ? 'bg-light text-primary' : 'bg-transparent text-dark'
              }`}
              onClick={() => handleNavigation(item.path)}
            >
              <i className={`bi ${item.icon} me-2`} />
              {item.label}
            </button>
          ))}
        </nav>

        <div className="px-3 py-3 border-top mt-auto">
          <small className="text-muted">© {new Date().getFullYear()} RRHH</small>
          <button 
            className="btn btn-link text-danger text-decoration-none p-0 d-block mt-2"
            onClick={() => {
              localStorage.removeItem('user');
              navigate('/');
            }}
          >
            <i className="bi bi-box-arrow-left me-2"></i>
            Cerrar Sesión
          </button>
        </div>
      </aside>
    </>
  );
}