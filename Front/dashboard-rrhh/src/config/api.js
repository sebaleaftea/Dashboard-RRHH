// Configuración de URLs de APIs

// Siempre usar Cloud Run (los servicios ya no están en localhost)
export const API_URLS = {
  // Microservice User (autenticación)
  USER_SERVICE: 'https://microservice-user-7jrwjdnsoq-tl.a.run.app',
  
  // Microservice Employee (empleados)
  EMPLOYEE_SERVICE: 'https://microservice-employee-7jrwjdnsoq-tl.a.run.app',
};

// Endpoints específicos
export const ENDPOINTS = {
  LOGIN: `${API_URLS.USER_SERVICE}/api/user/login`,
  USERS: `${API_URLS.USER_SERVICE}/api/user/all`,
  CREATE_USER: `${API_URLS.USER_SERVICE}/api/user/create`,
  
  EMPLOYEES: `${API_URLS.EMPLOYEE_SERVICE}/api/db/empleados`,
  EMPLOYEE_DETAIL: (id) => `${API_URLS.EMPLOYEE_SERVICE}/api/db/empleados/${id}/detalle`,
};
