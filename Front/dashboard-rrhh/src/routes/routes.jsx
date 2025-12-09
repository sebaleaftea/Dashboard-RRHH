
import { Navigate } from "react-router-dom";
import { BrowserRouter, Routes, Route} from "react-router-dom";
import Login from "../pages/Login";
import Home from "../pages/Home";

const PrivateRoute = ({ children }) => {
  const user = localStorage.getItem('user');
  return user ? children : <Navigate to="/" />;
};

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/home" element={<PrivateRoute><Home /></PrivateRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}