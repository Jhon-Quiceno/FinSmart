// Datos mock para Servicios (pagos recurrentes) — reemplazar por GET /api/recurring-payments
// (TODO Fase 0 backend). Forma replica RecurringPayment de smart-finance-frontend/lib/types.

export type RecurringFrequency = 'MONTHLY' | 'WEEKLY';

export interface MockRecurringPayment {
  id: number;
  name: string;
  amount: number;
  frequency: RecurringFrequency;
  nextPaymentDate: string;
  isActive: boolean;
}

export const mockRecurringPayments: MockRecurringPayment[] = [
  { id: 1, name: 'Netflix', amount: 44_900, frequency: 'MONTHLY', nextPaymentDate: '2026-09-01', isActive: true },
  { id: 2, name: 'Spotify', amount: 19_900, frequency: 'MONTHLY', nextPaymentDate: '2026-08-28', isActive: true },
  { id: 3, name: 'Gimnasio', amount: 120_000, frequency: 'MONTHLY', nextPaymentDate: '2026-09-05', isActive: true },
  { id: 4, name: 'Plan de celular', amount: 65_000, frequency: 'MONTHLY', nextPaymentDate: '2026-08-30', isActive: false },
];

export const FREQUENCY_LABELS: Record<RecurringFrequency, string> = {
  MONTHLY: 'Mensual',
  WEEKLY: 'Semanal',
};
