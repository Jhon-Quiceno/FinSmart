// Datos mock para el pase de diseño del Dashboard — reemplazar por GET /api/analysis/summary
// (TODO Fase 0 backend) una vez exista el cliente HTTP mobile-friendly.

export type TransactionType = 'INCOME' | 'EXPENSE';
export type RecommendationSeverity = 'HIGH' | 'MEDIUM' | 'LOW';

export interface MockTopCategory {
  categoryName: string;
  totalAmount: number;
  percentage: number;
  /** Meta mensual de la categoría — para mostrar "$gastado / $meta" en vez de solo lo gastado. */
  budget: number;
}

export interface MockRecentTransaction {
  id: number;
  type: TransactionType;
  amount: number;
  description: string;
  date: string;
  categoryName: string | null;
}

export interface MockRecommendation {
  severity: RecommendationSeverity;
  message: string;
}

export const mockDashboardSummary = {
  periodYear: 2026,
  periodMonth: 8,
  totalIncome: 4_800_000,
  totalExpense: 3_120_450,
  savings: 1_679_550,
  expenseRatio: 0.65,
  expenseAlert: false,
  totalDebt: 2_450_000,
  debtRatio: 0.51,
  topCategories: [
    { categoryName: 'Mercado', totalAmount: 820_000, percentage: 0.26, budget: 1_000_000 },
    { categoryName: 'Transporte', totalAmount: 410_000, percentage: 0.13, budget: 500_000 },
    { categoryName: 'Servicios', totalAmount: 355_000, percentage: 0.11, budget: 355_000 },
  ] satisfies MockTopCategory[],
  monthlySeries: [
    { month: 3, totalIncome: 4_500_000, totalExpense: 3_400_000 },
    { month: 4, totalIncome: 4_600_000, totalExpense: 3_050_000 },
    { month: 5, totalIncome: 4_750_000, totalExpense: 3_600_000 },
    { month: 6, totalIncome: 4_800_000, totalExpense: 2_980_000 },
    { month: 7, totalIncome: 4_700_000, totalExpense: 3_250_000 },
    { month: 8, totalIncome: 4_800_000, totalExpense: 3_120_450 },
  ],
};

export const mockRecentTransactions: MockRecentTransaction[] = [
  { id: 1, type: 'EXPENSE', amount: 185_000, description: 'Supermercado La Rebaja', date: '2026-08-19', categoryName: 'Mercado' },
  { id: 2, type: 'INCOME', amount: 4_800_000, description: 'Nómina agosto', date: '2026-08-15', categoryName: 'Salario' },
  { id: 3, type: 'EXPENSE', amount: 62_500, description: 'Uber al trabajo', date: '2026-08-14', categoryName: 'Transporte' },
  { id: 4, type: 'EXPENSE', amount: 149_900, description: 'Netflix + Spotify', date: '2026-08-12', categoryName: 'Suscripciones' },
  { id: 5, type: 'EXPENSE', amount: 320_000, description: 'Factura de luz', date: '2026-08-10', categoryName: 'Servicios' },
];

export const mockRecommendations: MockRecommendation[] = [
  { severity: 'MEDIUM', message: 'Vas 8% arriba del gasto en "Mercado" comparado con julio.' },
  { severity: 'LOW', message: 'Tu proyección de ahorro anual mejoró 12% este mes.' },
];
