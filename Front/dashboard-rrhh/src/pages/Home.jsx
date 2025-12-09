import { useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import  NavBar  from '../components/Navbar';

export default function Home() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeSection, setActiveSection] = useState('dashboard');

  return (
    <div className="min-vh-100 bg-light d-flex">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        activeSection={activeSection}
        onSectionChange={setActiveSection}
      />

      <div className="flex-grow-1 d-flex flex-column">
        <NavBar onMenuClick={() => setSidebarOpen((v) => !v)} />
        <main className="flex-grow-1 p-3">
          {/* Aquí renderiza el contenido según la sección activa */}
          <div className="card">
            <div className="card-body">
              {activeSection === 'dashboard' && <div>Dashboard</div>}
              {activeSection === 'empleados' && <div>Empleados</div>}
              {activeSection === 'reportes' && <div>Reportes</div>}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}