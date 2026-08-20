// Datos mock para Categorías — reemplazar por GET /api/categories (TODO Fase 0: falta el
// cliente HTTP mobile-friendly; el CRUD de categorías ya existe en el backend real).
// Nombres coherentes con los usados en mock/movimientos.ts y mock/dashboard.ts.

export type CategoryType = 'INCOME' | 'EXPENSE';

export interface MockCategory {
  id: number;
  name: string;
  type: CategoryType;
}

export const mockCategories: MockCategory[] = [
  { id: 1, name: 'Mercado', type: 'EXPENSE' },
  { id: 2, name: 'Transporte', type: 'EXPENSE' },
  { id: 3, name: 'Suscripciones', type: 'EXPENSE' },
  { id: 4, name: 'Servicios', type: 'EXPENSE' },
  { id: 5, name: 'Restaurantes', type: 'EXPENSE' },
  { id: 6, name: 'Salud', type: 'EXPENSE' },
  { id: 7, name: 'Salario', type: 'INCOME' },
  { id: 8, name: 'Otros ingresos', type: 'INCOME' },
];
