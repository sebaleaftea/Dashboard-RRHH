// Mapa de cargos antiguos a la nueva nomenclatura
export const MAPA_CARGOS = {
  'Vendedor-Reponedor-Cajero': 'Operador de tienda',
  'Vendedor Reponedor Cajero 20': 'Operador de tienda 16',
  'Vendedor Reponedor Cajero 30': 'Operador de tienda 24',
  'Administrador Jr Trainee': 'Administrador de tienda Jr',
  'SUB-ENCARGADO DE TIENDA': 'Administrador de tienda Jr',
  'Operador de tienda Trainee': 'Operador de tienda',
};

export function normalizarCargo(cargo) {
  if (!cargo) return '';
  return MAPA_CARGOS[cargo] || cargo;
}
