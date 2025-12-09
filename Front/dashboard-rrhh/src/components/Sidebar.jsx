// ...existing code...
// Carga de CSS global debe hacerse en main.jsx; eliminar imports locales
// import 'bootstrap/dist/css/bootstrap.min.css';
// import 'bootstrap-icons/font/bootstrap-icons.css';

export function Sidebar({ isOpen, onClose, activeSection, onSectionChange }) {
  const items = [
    { key: 'dashboard', icon: 'bi-speedometer2', label: 'Dashboard' },
    { key: 'empleados', icon: 'bi-people', label: 'Empleados' },
    { key: 'reportes', icon: 'bi-bar-chart', label: 'Reportes' },
    { key: 'item x', icon: 'bi-bar-chart', label: 'item x' },
    { key: 'item y', icon: 'bi-bar-chart', label: 'item y' },
    { key: 'item z', icon: 'bi-bar-chart', label: 'item z' },
    { key: 'item f', icon: 'bi-bar-chart', label: 'item f' },
  ];

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
                activeSection === item.key ? 'bg-light text-primary' : 'bg-transparent text-dark'
              }`}
              onClick={() => onSectionChange(item.key)}
            >
              <i className={`bi ${item.icon} me-2`} />
              {item.label}
            </button>
          ))}
        </nav>

        <div className="px-3 py-3 border-top mt-auto">
          <small className="text-muted">© {new Date().getFullYear()} RRHH</small>
        </div>
      </aside>

      {/* Separador a la izquierda para desktop */}
      <div className="d-none d-lg-block" style={{ width: '280px' }} />
    </>
  );
}