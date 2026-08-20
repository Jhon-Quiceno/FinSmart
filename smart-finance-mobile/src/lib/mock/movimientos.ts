// Datos mock para Ingresos/Gastos — reemplazar por GET /api/expenses y GET /api/incomes
// (TODO Fase 0 backend). La forma replica Expense/Income de smart-finance-frontend/lib/types.

export type PaymentMethodType = 'CASH' | 'DEBIT_CARD' | 'CREDIT_CARD' | 'TRANSFER' | 'OTHER';

export interface MockMovement {
  id: number;
  type: 'INCOME' | 'EXPENSE';
  amount: number;
  description: string;
  date: string;
  paymentMethod: PaymentMethodType | null;
  categoryName: string | null;
}

export const mockMovements: MockMovement[] = [
  { id: 1, type: 'EXPENSE', amount: 185_000, description: 'Supermercado La Rebaja', date: '2026-08-19', paymentMethod: 'DEBIT_CARD', categoryName: 'Mercado' },
  { id: 2, type: 'INCOME', amount: 4_800_000, description: 'Nómina agosto', date: '2026-08-15', paymentMethod: 'TRANSFER', categoryName: 'Salario' },
  { id: 3, type: 'EXPENSE', amount: 62_500, description: 'Uber al trabajo', date: '2026-08-14', paymentMethod: 'CREDIT_CARD', categoryName: 'Transporte' },
  { id: 4, type: 'EXPENSE', amount: 149_900, description: 'Netflix + Spotify', date: '2026-08-12', paymentMethod: 'CREDIT_CARD', categoryName: 'Suscripciones' },
  { id: 5, type: 'EXPENSE', amount: 320_000, description: 'Factura de luz', date: '2026-08-10', paymentMethod: 'TRANSFER', categoryName: 'Servicios' },
  { id: 6, type: 'INCOME', amount: 350_000, description: 'Venta bicicleta usada', date: '2026-08-08', paymentMethod: 'CASH', categoryName: 'Otros ingresos' },
  { id: 7, type: 'EXPENSE', amount: 98_000, description: 'Cena con amigos', date: '2026-08-05', paymentMethod: 'DEBIT_CARD', categoryName: 'Restaurantes' },
  { id: 8, type: 'EXPENSE', amount: 45_000, description: 'Farmacia', date: '2026-08-03', paymentMethod: 'CASH', categoryName: 'Salud' },
];

export const PAYMENT_METHOD_LABELS: Record<PaymentMethodType, string> = {
  CASH: 'Efectivo',
  DEBIT_CARD: 'Débito',
  CREDIT_CARD: 'Crédito',
  TRANSFER: 'Transferencia',
  OTHER: 'Otro',
};
