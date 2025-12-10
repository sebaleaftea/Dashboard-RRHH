import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/login.css';
import logo from '../assets/logo.png';

export default function Login() {
  const navigate = useNavigate();
  // Cambiamos el estado de email a name
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const response = await fetch('http://localhost:8081/api/user/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        // Enviamos 'name' en lugar de 'email'
        body: JSON.stringify({ 
          name: name, 
          passwordHash: password 
        }),
      });

      if (response.ok) {
        const userData = await response.json();
        localStorage.setItem('user', JSON.stringify(userData));
        navigate('/home');
      } else {
        setError('Usuario o contraseña incorrectos.');
      }
    } catch (err) {
      console.error("Error de conexión:", err);
      setError('Error al conectar con el servidor.');
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <img src={logo} alt="Logo" className="login-logo" />
          <h2 className="login-title">Bienvenido</h2>
          <p className="login-subtitle">Ingresa tus credenciales para continuar</p>
        </div>

        <form onSubmit={handleLogin} className="login-form">
          <div className="form-group">
            {/* Cambiamos la etiqueta y el input */}
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              type="text" // Tipo texto, no email
              className="form-control"
              placeholder="Ej: Pedrito23"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              className="form-control"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && <div className="alert alert-danger mt-3">{error}</div>}

          <button type="submit" className="btn btn-primary w-100 mt-4">
            Iniciar Sesión
          </button>
        </form>
      </div>
    </div>
  );
}