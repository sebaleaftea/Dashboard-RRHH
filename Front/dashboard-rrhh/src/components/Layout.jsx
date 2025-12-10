import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import NavBar from './Navbar';
import { Sidebar } from './Sidebar';

export default function Layout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
    <div style={{ minHeight: '100vh' }}>
      <Sidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />
      <div style={{ marginLeft: '280px' }}>
        <NavBar onMenuClick={() => setIsSidebarOpen(!isSidebarOpen)} />
        <main className="p-4">
          <div className="card shadow-sm">
            <div className="card-body">
              <Outlet />
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}