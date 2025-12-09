import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/login.css";

const Login = () => {
  const [form, setForm] = useState({ username: "", password: "" });
  const navigate = useNavigate();

  const isValid = form.username.trim() !== "" && form.password.trim() !== "";

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!isValid) {
      console.log("Login invalido");
      return;
    }
    navigate("/home");
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-title">Iniciar Sesion</h1>

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              name="username"
              type="text"
              value={form.username}
              onChange={handleChange}
              placeholder="ingrese su nombre de usuario"
              autoComplete="username"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              placeholder="••••••••"
              autoComplete="current-password"
              required
              minLength={6}
            />
          </div>

          <button type="submit" className="submit-btn" disabled={!isValid}>
            Ingresar
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;