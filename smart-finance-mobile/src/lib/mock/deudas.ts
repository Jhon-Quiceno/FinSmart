// Datos mock para Deudas/Tarjetas — reemplazar por GET /api/debts y GET /api/credit-cards
// (TODO Fase 0 backend). Formas replican Debt/CreditCard de smart-finance-frontend/lib/types.

export type CardFranchise = 'VISA' | 'MASTERCARD' | 'AMEX' | 'DINERS';

export interface MockDebt {
  id: number;
  name: string;
  totalAmount: number;
  remainingAmount: number;
  interestRate: number | null;
  dueDate: string | null;
}

export interface MockCreditCard {
  id: number;
  name: string;
  bank: string | null;
  franchise: CardFranchise;
  creditLimit: number;
  currentBalance: number;
  availableCredit: number;
  paymentDueDay: number;
}

export const mockDebts: MockDebt[] = [
  { id: 1, name: 'Préstamo libre inversión', totalAmount: 6_000_000, remainingAmount: 2_450_000, interestRate: 1.8, dueDate: '2027-02-01' },
  { id: 2, name: 'Crédito de estudio', totalAmount: 3_500_000, remainingAmount: 0, interestRate: 1.2, dueDate: null },
];

export const mockCreditCards: MockCreditCard[] = [
  { id: 1, name: 'Visa Signature', bank: 'Bancolombia', franchise: 'VISA', creditLimit: 5_000_000, currentBalance: 1_850_000, availableCredit: 3_150_000, paymentDueDay: 15 },
  { id: 2, name: 'Mastercard Gold', bank: 'Davivienda', franchise: 'MASTERCARD', creditLimit: 3_000_000, currentBalance: 2_700_000, availableCredit: 300_000, paymentDueDay: 5 },
];
