// Configuración de URLs de APIs

const isDevelopment = import.meta.env.MODE === 'development';

export const API_URLS = {
  // Microservice User (autenticación)
  USER_SERVICE: isDevelopment 
    ? 'http://localhost:8081'
    : 'https://microservice-user-7jrwjdnsoq-tl.a.run.app',
  
  // Microservice Employee (empleados)
  EMPLOYEE_SERVICE: isDevelopment 
    ? 'http://localhost:8082'
    : 'http://localhost:8082', // Cambiar cuando se despliegue
};

// Endpoints específicos
export const ENDPOINTS = {
  LOGIN: `${API_URLS.USER_SERVICE}/api/user/login`,
  USERS: `${API_URLS.USER_SERVICE}/api/user/all`,
  CREATE_USER: `${API_URLS.USER_SERVICE}/api/user/create`,
  
  EMPLOYEES: `${API_URLS.EMPLOYEE_SERVICE}/api/db/empleados`,
  EMPLOYEE_DETAIL: (id) => `${API_URLS.EMPLOYEE_SERVICE}/api/db/empleados/${id}/detalle`,
};
